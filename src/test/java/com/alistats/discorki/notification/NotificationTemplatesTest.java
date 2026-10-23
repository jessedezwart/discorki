package com.alistats.discorki.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.alistats.discorki.model.Division;
import com.alistats.discorki.model.League;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.model.Tier;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;
import com.alistats.discorki.notification.team_post_game.CCScoreNotification;
import com.alistats.discorki.notification.team_post_game.ClashGameFinishedNotification;
import com.alistats.discorki.notification.team_post_game.OutdamagedBySupportNotification;
import com.alistats.discorki.notification.team_post_game.PentaNotification;
import com.alistats.discorki.notification.team_post_game.PlayerYuumiNotification;
import com.alistats.discorki.notification.team_post_game.TankNotification;
import com.alistats.discorki.notification.team_post_game.TeamPostGameNotification;
import com.alistats.discorki.notification.team_post_game.TopDpsNotification;
import com.alistats.discorki.riot.MatchFixtures;

import io.pebbletemplates.pebble.PebbleEngine;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

/**
 * Runs the post game notifications against a match-v5 payload and renders their
 * templates in strict mode, so a template referencing a property R4J doesn't
 * have fails instead of silently rendering nothing.
 */
class NotificationTemplatesTest {
    private final PebbleEngine engine = new PebbleEngine.Builder()
            .autoEscaping(false)
            .strictVariables(true)
            .build();

    private final LOLMatch match = MatchFixtures.match();

    @Test
    void topDps() {
        assertTeamNotification(new TopDpsNotification(), match, "puuid-sup-red");
    }

    @Test
    void tank() {
        assertTeamNotification(new TankNotification(), match, "puuid-top-red");
    }

    @Test
    void ccScore() {
        String rendered = assertTeamNotification(new CCScoreNotification(), match, "puuid-top-red");
        assertTrue(rendered.contains("That's 13 per minute"), rendered);
    }

    @Test
    void penta() {
        assertTeamNotification(new PentaNotification(), match, "puuid-adc-blue", "puuid-mid-blue");
    }

    @Test
    void pentaIsNotSentWithoutPenta() {
        assertTrue(new PentaNotification().check(match, tracked(match, "puuid-mid-blue")).isEmpty());
    }

    @Test
    void yuumi() {
        assertTeamNotification(new PlayerYuumiNotification(), match, "puuid-sup-blue");
    }

    @Test
    void outdamagedBySupport() {
        String rendered = assertTeamNotification(new OutdamagedBySupportNotification(), match,
                "puuid-adc-blue", "puuid-sup-blue");
        assertTrue(rendered.contains("Blue Adc just got outdamaged by his support Blue Sup"), rendered);
    }

    @Test
    void clashGameFinished() {
        LOLMatch clashMatch = MatchFixtures.match(json -> json.addProperty("queueId", 700));
        assertTeamNotification(new ClashGameFinishedNotification(), clashMatch, "puuid-top-blue");
    }

    @Test
    void longGameTemplate() {
        MatchParticipant participant = MatchFixtures.participant(match, "puuid-top-blue");
        String rendered = render("LongGameNotification", Map.of("participant", participant, "match", match,
                "champion", "Garen", "extraArgs", Map.of()));
        assertTrue(rendered.contains("The game took **30 minutes**"), rendered);
    }

    @Test
    void lostAgainstBotsTemplate() {
        MatchParticipant participant = MatchFixtures.participant(match, "puuid-top-blue");
        String rendered = render("LostAgainstBotsNotification", Map.of("participant", participant, "match", match,
                "champion", "Garen", "extraArgs", Map.of()));
        assertTrue(rendered.contains("Blue Top"), rendered);
    }

    @Test
    void levelTemplate() {
        Summoner summoner = summoner("puuid-top-blue");
        summoner.setLevel(500);
        String rendered = render("LevelNotification", Map.of("summoner", summoner, "match", match,
                "extraArgs", Map.of()));
        assertTrue(rendered.contains("Blue Top#EUW achieved the amazing milestone of summoner level 500"), rendered);
    }

    @Test
    void rankChangedTemplate() {
        Rank rank = new Rank();
        rank.setQueueType(QueueType.RANKED_SOLO_5x5);
        rank.setLeague(new League(Division.II, Tier.EMERALD));
        String rendered = render("RankChangedNotification", Map.of("summoner", summoner("puuid-top-blue"),
                "match", match, "extraArgs", Map.of("promoted", true, "newRank", rank,
                        "queueDescription", QueueType.RANKED_SOLO_5x5.getName())));
        assertTrue(rendered.contains("promoted to **Emerald II** in **Ranked Solo/Duo**"), rendered);
    }

    private String assertTeamNotification(TeamPostGameNotification notification, LOLMatch match,
            String... trackedPuuids) {
        Optional<TeamPostGameNotificationResult> result = notification.check(match, tracked(match, trackedPuuids));
        assertTrue(result.isPresent(), "Expected a notification");
        assertTrue(!result.get().getSubjects().isEmpty(), "Expected a subject");

        StringBuilder rendered = new StringBuilder();
        // Same arguments as EmbedFactory#getTeamPostGameNotificationEmbeds
        result.get().getSubjects().forEach((summoner, participant) -> {
            String output = render(((Notification) notification).getName(), Map.of(
                    "participant", participant,
                    "match", match,
                    "champion", participant.getChampionName(),
                    "extraArgs", result.get().getExtraArguments()));
            rendered.append(output);
        });
        return rendered.toString();
    }

    private HashMap<Summoner, MatchParticipant> tracked(LOLMatch match, String... puuids) {
        HashMap<Summoner, MatchParticipant> tracked = new HashMap<>();
        Arrays.stream(puuids).forEach(puuid -> tracked.put(summoner(puuid), MatchFixtures.participant(match, puuid)));
        return tracked;
    }

    private Summoner summoner(String puuid) {
        MatchParticipant participant = MatchFixtures.participant(match, puuid);
        Summoner summoner = new Summoner();
        summoner.setPuuid(puuid);
        summoner.setGameName(participant.getRiotIdName());
        summoner.setTagLine(participant.getRiotIdTagline());
        return summoner;
    }

    private String render(String notificationName, Map<String, Object> context) {
        try {
            StringWriter writer = new StringWriter();
            engine.getTemplate("templates/notifications/" + notificationName + ".pebble").evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new AssertionError("Could not render " + notificationName, e);
        }
    }
}
