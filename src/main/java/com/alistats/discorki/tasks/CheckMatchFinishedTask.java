package com.alistats.discorki.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.alistats.discorki.riot.MatchUtil;
import com.alistats.discorki.riot.controller.LeagueApiHelper;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import no.stelar7.api.r4j.basic.exceptions.APIEnumNotUpToDateException;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class CheckMatchFinishedTask extends Task {
    @Autowired
    private List<TeamPostGameNotification> teamNotificationCheckers;
    @Autowired
    private List<PersonalPostGameNotification> personalNotificationCheckers;
    @Autowired
    private LeagueApiHelper apiHelper;
    @Autowired
    private CacheManager cacheManager;

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
            LOLMatch matchDto = leagueApiController.getMatch(match.getId());
            if (matchDto == null) {
                LOG.info("Game {} is not finished yet.", match.getId());
                return;
            }

            updateRiotIds(matchDto, summoners);

            if (MatchUtil.isAborted(matchDto)) {
                LOG.info("Game {} is finished, but seems to be a remake, not checking!", match.getId());
            } else {
                LOG.info("Game {} is finished, checking for notable events...", match.getId());
                checkForNotableEvents(matchDto, summoners);
            }
        } catch (APIEnumNotUpToDateException e) {
            // The game contains a queue, map or similar that R4J doesn't know yet. Retrying won't help.
            LOG.warn("Game {} can't be parsed, skipping it. Is R4J outdated? {}", match.getId(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Error while checking if game {} is finished.", match.getId(), e);
            return;
        }

        LOG.info("Setting {} to finished.", match.getId());
        match.setStatus(Status.FINISHED);
        matchRepo.save(match);
    }

    /**
     * Riot IDs can be changed by players, keep them up to date with the latest match.
     */
    private void updateRiotIds(LOLMatch match, Set<Summoner> summoners) {
        for (MatchParticipant participant : match.getParticipants()) {
            summoners.stream()
                    .filter(summoner -> summoner.getPuuid().equals(participant.getPuuid()))
                    .filter(summoner -> participant.getRiotIdName() != null
                            && !summoner.hasRiotId(participant.getRiotIdName(), participant.getRiotIdTagline()))
                    .forEach(summoner -> {
                        LOG.info("Riot ID of {} changed to {}", summoner.getName(), MatchUtil.getRiotId(participant));
                        summoner.setGameName(participant.getRiotIdName());
                        summoner.setTagLine(participant.getRiotIdTagline());
                        summonerRepo.save(summoner);
                    });
        }
    }

    private void checkForNotableEvents(LOLMatch match, Set<Summoner> trackedPlayers) {
        // Clear rank cache
        cacheManager.getCache("ranks").clear();

        HashMap<Summoner, MatchParticipant> trackedParticipantsMap = mapTrackedParticipants(trackedPlayers,
                match.getParticipants());

        EmbedContainer embeds = new EmbedContainer();

        // For each tracked and participating summoner, check for personal notifications
        for (Summoner summoner : trackedPlayers) {
            personalNotificationCheckers
                    .forEach(checker -> {
                        LOG.info("Checking for '{}'  for {}", checker.getClass().getSimpleName(),
                                summoner.getName());
                        Optional<PersonalPostGameNotificationResult> result = checker.check(match, summoner);
                        result.ifPresent(personalPostGameNotificationResult -> embeds.addPersonalEmbed(summoner, embedFactory.getPersonalPostGameNotificationEmbed(personalPostGameNotificationResult)));
                    });
        }

        // Check for team notifications
        teamNotificationCheckers.forEach(checker -> {
            LOG.info("Checking for '{}' for {}", checker.getClass().getSimpleName(),
                    match.getGameId());
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
            LOG.info("No notable events found for {}", match.getGameId());
            return;
        }

        // Get participants' ranks if one team notification was found
        Map<String, Rank> participantRanks = new HashMap<>();
        if (embeds.hasTeamEmbeds()) {
            LOG.info("Getting ranks for {}", match.getGameId());
            participantRanks = apiHelper.getParticipantRanks(match.getParticipants());
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
                    continue;
                }
                LOG.info("Sending {} embeds to channel {} in guild {}", guildEmbedList.size(), channel.getName(),
                        server.getName());
                // Discord allows up to 10 embeds per message
                for (int i = 0; i < guildEmbedList.size(); i += Message.MAX_EMBED_COUNT) {
                    channel.sendMessageEmbeds(guildEmbedList.subList(i,
                            Math.min(i + Message.MAX_EMBED_COUNT, guildEmbedList.size()))).queue();
                }
            } catch (Exception e) {
                LOG.error("Error while building/sending embeds for game {}: {}", match.getGameId(),
                        e.getMessage());
            }
        }

    }

    public HashMap<Summoner, MatchParticipant> mapTrackedParticipants(Set<Summoner> trackedSummoners,
            List<MatchParticipant> participants) {
        HashMap<Summoner, MatchParticipant> summonerParticipantMap = new HashMap<>();
        participants.forEach(participant -> trackedSummoners.stream()
                .filter(summoner -> summoner.getPuuid().equals(participant.getPuuid()))
                .forEach(summoner -> summonerParticipantMap.put(summoner, participant)));

        return summonerParticipantMap;
    }
}
