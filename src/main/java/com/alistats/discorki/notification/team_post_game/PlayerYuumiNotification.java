package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class PlayerYuumiNotification extends Notification implements TeamPostGameNotification {
    @Override
    public String getName() {
        return "PlayerPlayedYuumiNotification";
    }
    @Override
    public String getFancyName() {
        return "Player played Yuumi notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a player played Yuumi.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants) {
        HashMap<Summoner, MatchParticipant> subjects = new HashMap<>();
        // Check for tracked summoners if they got a penta
        for (Summoner summoner : trackedParticipants.keySet()) {
            // Check if participant is yuumi
            MatchParticipant participant = trackedParticipants.get(summoner);
            if (participant.getChampionName().equals("Yuumi")) {
                subjects.put(summoner, participant);
            }
        }

        if (subjects.size() > 0) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(subjects);
            result.setTitle("Yuumi!");

            return Optional.of(result);
        }

        return Optional.empty();
    }
}
