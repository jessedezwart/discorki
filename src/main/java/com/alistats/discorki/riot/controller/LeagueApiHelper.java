package com.alistats.discorki.riot.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.alistats.discorki.model.Rank;
import com.alistats.discorki.riot.dto.LeagueEntryDto;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.ParticipantDto;

@Component
public class LeagueApiHelper {
    @Autowired private LeagueApiController apiController;

    public MatchDto getMostRecentMatch(String puuid)
            throws HttpClientErrorException, HttpServerErrorException {
        String matchId = apiController.getMostRecentMatchId(puuid);
        if (matchId == null) {
            return null;
        }

        // Strip match id of platform prefix
        matchId = matchId.substring(matchId.indexOf("_") + 1);

        return apiController.getMatch(Long.parseLong(matchId));
    }

    /**
     * Gets the rank of all participants in a match, favoring soloq over flex
     * @param participants The participants in the match
     * @return A map of participants to their ranks
     */
    public HashMap<ParticipantDto, Rank> getParticipantRanks(ParticipantDto[] participants) {
        HashMap<ParticipantDto, Rank> participantRanks = new HashMap<>();

        // fetch all the soloq ranks off all team members
        for (ParticipantDto participant : participants) {
            LeagueEntryDto[] leagueEntries = apiController.getLeagueEntriesByPuuid(participant.getPuuid());
            if (leagueEntries != null) {
                LeagueEntryDto rankedSolo = null;
                LeagueEntryDto rankedFlex = null;
                for (LeagueEntryDto leagueEntry : leagueEntries) {
                    if (leagueEntry.getQueueType().equals("RANKED_SOLO_5x5")) {
                        rankedSolo = leagueEntry;
                        break;
                    } else if (leagueEntry.getQueueType().equals("RANKED_FLEX_SR")) {
                        rankedFlex = leagueEntry;
                    }
                }
                if (rankedSolo != null) {
                    try {
                        participantRanks.put(participant, rankedSolo.toRank());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (rankedFlex != null) {
                    try {
                        participantRanks.put(participant, rankedFlex.toRank());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return participantRanks;
    }
}
