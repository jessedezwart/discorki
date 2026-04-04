package com.alistats.discorki.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.JDASingleton;
import com.alistats.discorki.model.EmbedContainer;
import com.alistats.discorki.model.Match;
import com.alistats.discorki.model.Match.Status;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.personal_post_game.PersonalPostGameNotification;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;
import com.alistats.discorki.notification.team_post_game.TeamPostGameNotification;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.discord.EmbedFactory;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.controller.LeagueApiHelper;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.ParticipantDto;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@Component
public class CheckMatchFinishedTask extends Task {
    private final List<TeamPostGameNotification> teamNotificationCheckers;
    private final List<PersonalPostGameNotification> personalNotificationCheckers;
    private final LeagueApiHelper apiHelper;
    private final CacheManager cacheManager;

    public CheckMatchFinishedTask(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            MatchRepo matchRepo, ServerRepo serverRepo, EmbedFactory embedFactory,
            List<TeamPostGameNotification> teamNotificationCheckers,
            List<PersonalPostGameNotification> personalNotificationCheckers, LeagueApiHelper apiHelper,
            CacheManager cacheManager) {
        super(leagueApiController, summonerRepo, matchRepo, serverRepo, embedFactory);
        this.teamNotificationCheckers = teamNotificationCheckers;
        this.personalNotificationCheckers = personalNotificationCheckers;
        this.apiHelper = apiHelper;
        this.cacheManager = cacheManager;
    }

    // Run every minute at second :30
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void checkMatchesFinished() {
        LOG.info("Running task {}", this.getClass().getSimpleName());

        // Get all matches in progress
        Set<Match> matchesInProgress = matchRepo.findByStatus(Status.IN_PROGRESS).orElseThrow();
        LOG.info("Found {} matches in progress.", matchesInProgress.size());

        // Check if the games are finished
        for (Match match : matchesInProgress) {
            checkMatchFinished(match);
        }
    }

    private void checkMatchFinished(Match match) {
        LOG.info("Checking if game {} is finished...", match.getId());
        Set<Summoner> summoners = match.getTrackedSummoners();
        try {
            MatchDto matchDto = leagueApiController.getMatch(match.getId());
            if (matchDto.getInfo().isAborted()) {
                LOG.info("Game {} is finished, but seems to be a remake, not checking!", match.getId());
            } else {
                LOG.info("Game {} is finished, checking for notable events...", match.getId());
                checkForNotableEvents(matchDto, summoners);
            }
            LOG.info("Setting {} to finished.", match.getId());
            match.setStatus(Status.FINISHED);
            matchRepo.save(match);
        } catch (Exception e) {
            if (e.getMessage().contains("404")) {
                LOG.info("Game {} is not finished yet.", match.getId());
            } else {
                LOG.error("Error while checking if game {} is finished. {}", match.getId(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void checkForNotableEvents(MatchDto match, Set<Summoner> trackedPlayers) {
        // Clear rank cache
        cacheManager.getCache("ranks").clear();

        HashMap<Summoner, ParticipantDto> trackedParticipantsMap = mapTrackedParticipants(trackedPlayers,
                match.getInfo().getParticipants());

        EmbedContainer embeds = new EmbedContainer();

        // For each tracked and participating summoner, check for personal notifications
        for (Summoner summoner : trackedPlayers) {
            personalNotificationCheckers
                    .forEach(checker -> {
                        LOG.info("Checking for '{}'  for {}", checker.getClass().getSimpleName(),
                                summoner.getRiotId());
                        Optional<PersonalPostGameNotificationResult> result = checker.check(match, summoner);
                        result.ifPresent(personalPostGameNotificationResult -> embeds.addPersonalEmbed(summoner, embedFactory.getPersonalPostGameNotificationEmbed(personalPostGameNotificationResult)));
                    });
        }

        // Check for team notifications
        teamNotificationCheckers.forEach(checker -> {
            LOG.info("Checking for '{}' for {}", checker.getClass().getSimpleName(),
                    match.getInfo().getGameId());
            Optional<TeamPostGameNotificationResult> result = checker.check(match, trackedParticipantsMap);
            if (result.isPresent()) {
                for (Summoner summoner : trackedPlayers) {
                    // check if summoner is in the key of hashmap subjects
                    if (!result.get().getSubjects().containsKey(summoner)) {
                        continue;
                    }
                    embeds.addTeamEmbeds(summoner, embedFactory.getTeamPostGameNotificationEmbeds(result.get()));
                }
            }
        });

        if (embeds.isEmpty()) {
            LOG.info("No notable events found for {}", match.getInfo().getGameId());
            return;
        }

        // Get participants' ranks if one team notification was found
        HashMap<ParticipantDto, Rank> participantRanks = new HashMap<>();
        if (embeds.hasTeamEmbeds()) {
            LOG.info("Getting ranks for {}", match.getInfo().getGameId());
            participantRanks = apiHelper.getParticipantRanks(match.getInfo().getParticipants());
        }

        // Initialize JDA
        JDA jda = JDASingleton.getJDA();
        for (Server server : embeds.getGuilds()) {
            LOG.info("Building embeds for guild {}", server.getName());
            List<MessageEmbed> guildEmbedList = new ArrayList<>(embeds.getGuildEmbeds(server));
            try {
                if (embeds.guildHasTeamEmbeds(server)) {
                    guildEmbedList.add(embedFactory.getMatchEmbed(server, match, participantRanks));
                }
                TextChannel channel = jda.getTextChannelById(server.getDefaultChannelId());
                if (channel == null) {
                    LOG.error("Could not find default channel for guild {}", server.getName());
                    return;
                }
                LOG.info("Sending {} embeds to channel {} in guild {}", guildEmbedList.size(), channel.getName(),
                        server.getName());
                channel.sendMessageEmbeds(guildEmbedList).queue();
            } catch (Exception e) {
                LOG.error("Error while building/sending embeds for game {}: {}", match.getInfo().getGameId(),
                        e.getMessage());
                return;
            }
        }

    }

    public HashMap<Summoner, ParticipantDto> mapTrackedParticipants(Set<Summoner> trackedSummoners,
            ParticipantDto[] participants) {
        Set<ParticipantDto> filteredParticipants = Arrays.stream(participants)
                .filter(p -> trackedSummoners.stream().anyMatch(s -> s.getPuuid().equals(p.getPuuid())))
                .collect(Collectors.toCollection(HashSet::new));

        HashMap<Summoner, ParticipantDto> summonerParticipantMap = new HashMap<>();
        filteredParticipants.forEach(participant -> trackedSummoners.stream()
                .filter(summoner -> summoner.getPuuid().equals(participant.getPuuid()))
                .forEach(summoner -> summonerParticipantMap.put(summoner, participant)));

        return summonerParticipantMap;
    }
}
