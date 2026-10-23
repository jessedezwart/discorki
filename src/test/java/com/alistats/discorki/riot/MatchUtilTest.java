package com.alistats.discorki.riot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.alistats.discorki.model.QueueType;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.basic.constants.types.lol.MapType;
import no.stelar7.api.r4j.basic.constants.types.lol.TeamType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;

class MatchUtilTest {

    @Test
    void readsMatchFromRiotPayload() {
        LOLMatch match = MatchFixtures.match();

        assertEquals(7000000001L, match.getGameId());
        assertEquals(MapType.SUMMONERS_RIFT, match.getMap());
        assertEquals(GameQueueType.TEAM_BUILDER_RANKED_SOLO, match.getQueue());
        assertEquals(LaneType.BOT, MatchFixtures.participant(match, "puuid-adc-blue").getChampionSelectLane());
        assertEquals(LaneType.MID, MatchFixtures.participant(match, "puuid-mid-blue").getChampionSelectLane());
        assertEquals("Blue Adc#EUW", MatchUtil.getRiotId(MatchFixtures.participant(match, "puuid-adc-blue")));
    }

    @Test
    void splitsParticipantsByTeam() {
        LOLMatch match = MatchFixtures.match();

        assertEquals(5, MatchUtil.getTeam(match, TeamType.BLUE).size());
        assertEquals(5, MatchUtil.getTeam(match, TeamType.RED).size());
        assertTrue(MatchUtil.getTeam(match, TeamType.RED).stream().allMatch(p -> p.getTeam() == TeamType.RED));
    }

    @Test
    void detectsRemakes() {
        assertFalse(MatchUtil.isAborted(MatchFixtures.match()));
        assertTrue(MatchUtil.isAborted(MatchFixtures.match(json -> json.addProperty("gameDuration", 170))));
        // ARAM games are remade sooner
        assertFalse(MatchUtil.isAborted(MatchFixtures.match(json -> {
            json.addProperty("mapId", 12);
            json.addProperty("gameDuration", 170);
        })));
    }

    @Test
    void mapsRankedQueues() {
        assertTrue(MatchUtil.isRanked(MatchFixtures.match()));
        assertFalse(MatchUtil.isRanked(MatchFixtures.match(json -> json.addProperty("queueId", 450))));

        assertEquals(QueueType.RANKED_SOLO_5x5, QueueType.from(GameQueueType.TEAM_BUILDER_RANKED_SOLO).orElseThrow());
        assertEquals(QueueType.RANKED_SOLO_5x5, QueueType.from(GameQueueType.RANKED_SOLO_5X5).orElseThrow());
        assertEquals(QueueType.RANKED_FLEX_SR, QueueType.from(GameQueueType.RANKED_FLEX_SR).orElseThrow());
        assertTrue(QueueType.from(GameQueueType.ARAM).isEmpty());
    }
}
