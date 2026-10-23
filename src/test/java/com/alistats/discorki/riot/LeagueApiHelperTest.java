package com.alistats.discorki.riot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.alistats.discorki.model.Division;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Tier;
import com.alistats.discorki.riot.controller.LeagueApiHelper;

import no.stelar7.api.r4j.basic.utils.Utils;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

class LeagueApiHelperTest {

    private static LeagueEntry entry(String queueType, String tier, String rank) {
        String json = """
                {"leagueId": "league", "puuid": "puuid", "queueType": "%s", "tier": "%s", "rank": "%s",
                 "leaguePoints": 42, "wins": 10, "losses": 8, "veteran": false, "inactive": false,
                 "freshBlood": false, "hotStreak": false}
                """.formatted(queueType, tier, rank);
        return Utils.getGson().fromJson(json, LeagueEntry.class);
    }

    @Test
    void convertsLeagueEntryToRank() {
        Rank rank = LeagueApiHelper.toRank(entry("RANKED_SOLO_5x5", "EMERALD", "II")).orElseThrow();

        assertEquals(QueueType.RANKED_SOLO_5x5, rank.getQueueType());
        assertEquals(Tier.EMERALD, rank.getLeague().getTier());
        assertEquals(Division.II, rank.getLeague().getDivision());
        assertEquals(42, rank.getLeaguePoints());
    }

    @Test
    void skipsUnsupportedQueues() {
        assertTrue(LeagueApiHelper.toRank(entry("RANKED_TFT", "GOLD", "I")).isEmpty());
    }
}
