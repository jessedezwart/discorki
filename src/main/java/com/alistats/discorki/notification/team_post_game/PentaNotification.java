package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

/**
 * A summoner got a penta in the last game
 */
@Component
public class PentaNotification extends Notification implements TeamPostGameNotification {
    @Override
    public String getName() {
        return "PentaNotification";
    }
    @Override
    public String getFancyName() {
        return "Penta notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner got a penta in the last game.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants) {
        HashMap<Summoner, MatchParticipant> subjects = new HashMap<>();
        for (Summoner summoner : trackedParticipants.keySet()) {
            MatchParticipant participant = trackedParticipants.get(summoner);
            if (participant.getPentaKills() > 0) {
                subjects.put(summoner, participant);
            }
        }

        // if there are any tracked participants left, notify
        if (!subjects.isEmpty()) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(subjects);
            result.setTitle("Penta!");

            return Optional.of(result);
        }

        return Optional.empty();
    }
}
