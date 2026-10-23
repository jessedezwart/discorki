package com.alistats.discorki.riot;

import java.util.List;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.constants.types.lol.MapType;
import no.stelar7.api.r4j.basic.constants.types.lol.TeamType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

/**
 * Helpers for match-v5 data that R4J doesn't provide.
 */
public final class MatchUtil {
    private static final int REMAKE_THRESHOLD_SECONDS_SR = 180;
    private static final int REMAKE_THRESHOLD_SECONDS_ARAM = 90;

    private MatchUtil() {
    }

    public static boolean isAborted(LOLMatch match) {
        if (match.getMap() == MapType.SUMMONERS_RIFT) {
            return match.getGameDuration() < REMAKE_THRESHOLD_SECONDS_SR;
        }
        return match.getGameDuration() < REMAKE_THRESHOLD_SECONDS_ARAM;
    }

    public static boolean isRanked(LOLMatch match) {
        return isRanked(match.getQueue());
    }

    public static boolean isRanked(GameQueueType queue) {
        return queue == GameQueueType.TEAM_BUILDER_RANKED_SOLO || queue == GameQueueType.RANKED_FLEX_SR;
    }

    public static List<MatchParticipant> getTeam(LOLMatch match, TeamType team) {
        return match.getParticipants().stream()
                .filter(participant -> participant.getTeam() == team)
                .toList();
    }

    public static long getDurationInMinutes(LOLMatch match) {
        return match.getGameDuration() / 60;
    }

    /**
     * @return the Riot ID of the participant, e.g. "Faker#KR1"
     */
    public static String getRiotId(MatchParticipant participant) {
        if (participant.getRiotIdTagline() == null || participant.getRiotIdTagline().isBlank()) {
            return participant.getRiotIdName();
        }
        return participant.getRiotIdName() + "#" + participant.getRiotIdTagline();
    }
}
