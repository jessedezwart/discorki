package com.alistats.discorki.riot.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Division;
import com.alistats.discorki.model.League;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Tier;

import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class LeagueApiHelper {
    private static final Logger LOG = LoggerFactory.getLogger(LeagueApiHelper.class);

    @Autowired private LeagueApiController apiController;

    public LOLMatch getMostRecentMatch(String puuid) {
        String matchId = apiController.getMostRecentMatchId(puuid);
        if (matchId == null) {
            return null;
        }

        return apiController.getMatch(matchId);
    }

    /**
     * Gets the rank of all participants in a match, favoring soloq over flex
     * @param participants The participants in the match
     * @return A map of participant puuids to their ranks
     */
    public Map<String, Rank> getParticipantRanks(List<MatchParticipant> participants) {
        Map<String, Rank> participantRanks = new HashMap<>();

        for (MatchParticipant participant : participants) {
            List<LeagueEntry> leagueEntries = apiController.getLeagueEntries(participant.getPuuid());
            if (leagueEntries == null) {
                continue;
            }

            Rank rankedSolo = null;
            Rank rankedFlex = null;
            for (LeagueEntry leagueEntry : leagueEntries) {
                Optional<Rank> rank = toRank(leagueEntry);
                if (rank.isEmpty()) {
                    continue;
                }
                if (rank.get().getQueueType() == QueueType.RANKED_SOLO_5x5) {
                    rankedSolo = rank.get();
                } else {
                    rankedFlex = rank.get();
                }
            }

            if (rankedSolo != null) {
                participantRanks.put(participant.getPuuid(), rankedSolo);
            } else if (rankedFlex != null) {
                participantRanks.put(participant.getPuuid(), rankedFlex);
            }
        }

        return participantRanks;
    }

    /**
     * Converts a league entry to a rank. The rank is not linked to a summoner.
     * @return the rank, or empty when the entry is not for a supported ranked queue
     */
    public static Optional<Rank> toRank(LeagueEntry leagueEntry) {
        Optional<QueueType> queueType = QueueType.from(leagueEntry.getQueueType());
        if (queueType.isEmpty()) {
            return Optional.empty();
        }

        try {
            League league = new League();
            league.setDivision(Division.valueOf(leagueEntry.getRank()));
            league.setTier(Tier.valueOf(leagueEntry.getTier()));

            Rank rank = new Rank();
            rank.setQueueType(queueType.get());
            rank.setLeague(league);
            rank.setLeaguePoints(leagueEntry.getLeaguePoints());
            return Optional.of(rank);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown tier or division {} {}", leagueEntry.getTier(), leagueEntry.getRank());
            return Optional.empty();
        }
    }
}
