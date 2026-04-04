package com.alistats.discorki.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.EmbedFactory;
import com.alistats.discorki.model.Match;
import com.alistats.discorki.model.Match.Status;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.game_start.GameStartNotification;
import com.alistats.discorki.notification.result.GameStartNotificationResult;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.dto.CurrentGameInfoDto;
import com.alistats.discorki.riot.dto.CurrentGameInfoDto.ParticipantDto;

import net.dv8tion.jda.api.entities.MessageEmbed;

@Component
public class CheckJustInGameTask extends Task {
    private final List<GameStartNotification> gameStartNotificationCheckers;

    public CheckJustInGameTask(LeagueApiController leagueApiController, SummonerRepo summonerRepo, MatchRepo matchRepo,
            ServerRepo serverRepo, EmbedFactory embedFactory, List<GameStartNotification> gameStartNotificationCheckers) {
        super(leagueApiController, summonerRepo, matchRepo, serverRepo, embedFactory);
        this.gameStartNotificationCheckers = gameStartNotificationCheckers;
    }

    // Run every 5 minutes at 0 seconds
    // Starts with a delay to make sure JDA is initialized. Later on a singleton is
    // used
    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    @Transactional
    public void checkJustInGame() {
        LOG.info("Running task {}", this.getClass().getSimpleName());

        // A list of summoners that a game was found for and won't be checked again
        Set<Summoner> skiplist = new HashSet<>();

        // Get all tracked summoners from the database
        // for each active guild, get all summoners and put them in a set
        Set<Summoner> summonersToCheck = serverRepo.findByActiveTrue().stream()
                .flatMap(g -> g.getSummoners().stream()).collect(Collectors.toSet());

        LOG.info("Got {} summoners to check from db", summonersToCheck.size());

        // Get all matches in progress from the database
        Set<Match> matchesInProgress = matchRepo.findByStatus(Status.IN_PROGRESS).orElseThrow();
        LOG.info("Got {} matches in progress from db", matchesInProgress.size());

        // Define temp game for storing the current game to reduce api calls
        AtomicReference<CurrentGameInfoDto> tempGame = new AtomicReference<>();

        summonersToCheck
                .stream()
                .filter(s -> !skiplist.contains(s))
                .filter(s -> {
                    Optional<CurrentGameInfoDto> currentGameInfoDtoOpt = getCurrentGame(s.getId());

                    if (currentGameInfoDtoOpt.isEmpty()) {
                        return false;
                    }

                    if (s.getMatchInProgress().map(Match::getId).orElse(-1L) == currentGameInfoDtoOpt.get()
                            .getGameId()) {
                        return false;
                    }

                    tempGame.set(currentGameInfoDtoOpt.get());
                    return true;
                })
                .forEach(s -> {
                    // Get participants from current game and check if other
                    // tracked summoners are in the game. If so, add to skiplist
                    CurrentGameInfoDto game = tempGame.get();
                    Set<Summoner> trackedSummonersInGame = filterTrackedSummoners(summonersToCheck,
                            game.getParticipants());

                    // Skip if no queue config id is found
                    if (game.getGameQueueConfigId() == 0) {
                        LOG.warn("No queue config id found for game {}", game.getGameId());
                        return;
                    }

                    // Create match object and save to database
                    Match match = new Match(game.getGameId(), trackedSummonersInGame, Status.IN_PROGRESS,
                            game.getGameQueueConfigId());
                    matchRepo.save(match);

                    LOG.info("Found new match {} for {} summoners", match.getId(),
                            match.getTrackedSummoners().size());

                    // Check for notable events
                    checkForNotableEvents(game, trackedSummonersInGame);

                    skiplist.addAll(trackedSummonersInGame);
                });

    }

    private Optional<CurrentGameInfoDto> getCurrentGame(String summonerId) {
        try {
            return Optional.of(leagueApiController.getCurrentGameInfo(summonerId));
        } catch (Exception e) {
            if (!e.getMessage().contains("404")) {
                LOG.error("Error getting current game for summoner {}", summonerId, e);
            }
            return Optional.empty();
        }
    }

    private void checkForNotableEvents(CurrentGameInfoDto currentGameInfo, Set<Summoner> trackedSummoners) {
        try {
            HashMap<Summoner, Set<MessageEmbed>> embeds = new HashMap<>();
            AtomicBoolean notificationFound = new AtomicBoolean(false);

            gameStartNotificationCheckers.forEach(checker -> {
                LOG.info("Checking for '{}' for {}", checker.getClass().getSimpleName(),
                        currentGameInfo.getGameId());
                Optional<GameStartNotificationResult> result = checker.check(currentGameInfo, trackedSummoners);

                if (result.isPresent()) {
                    notificationFound.set(true);
                    result.get().getSubjects().forEach((summoner, subjects) -> {
                        if (embeds.containsKey(summoner)) {
                            embeds.get(summoner).addAll(embedFactory.getGameStartNotificationEmbeds(result.get()));
                        } else {
                            embeds.put(summoner,
                                    new HashSet<>(embedFactory.getGameStartNotificationEmbeds(result.get())));
                        }
                    });
                }
            });

            if (!notificationFound.get()) {
                LOG.info("No notable events found for game {}", currentGameInfo.getGameId());
                return;
            }

            HashMap<Server, Set<MessageEmbed>> guildEmbeds = new HashMap<>();
            embeds.forEach((summoner, embed) -> summoner.getPlayers().forEach(user -> {
                Server server = user.getServer();
                guildEmbeds.computeIfAbsent(server, k -> new HashSet<>()).addAll(embed);
            }));

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
    }

    public Set<Summoner> filterTrackedSummoners(Set<Summoner> trackedSummoners,
            ParticipantDto[] participants) {
        Set<Summoner> trackedSummonersInGame = new HashSet<>();

        for (Summoner s : trackedSummoners) {
            for (ParticipantDto p : participants) {
                if (s.getPuuid().equals(p.getPuuid())) {
                    trackedSummonersInGame.add(s);
                }
            }
        }

        return trackedSummonersInGame;
    }

}
