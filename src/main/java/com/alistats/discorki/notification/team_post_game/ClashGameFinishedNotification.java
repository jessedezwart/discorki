package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class ClashGameFinishedNotification extends Notification implements TeamPostGameNotification {
    @Override
    public String getName() {
        return "ClashGameFinishedNotification";
    }
    @Override
    public String getFancyName() {
        return "Clash game finished notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a clash game has finished.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants) {
        // Check if the game is a clash game
        if (match.getQueue() == GameQueueType.CLASH) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(trackedParticipants);
            result.setTitle("Clash game finished");
            
            return Optional.of(result);
        }

        return Optional.empty();
    }
}
