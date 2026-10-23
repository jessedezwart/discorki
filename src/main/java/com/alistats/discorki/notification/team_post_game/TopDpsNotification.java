package com.alistats.discorki.notification.team_post_game;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class TopDpsNotification extends Notification implements TeamPostGameNotification {

    @Override
    public String getName() {
        return "TopDpsNotification";
    }
    @Override
    public String getFancyName() {
        return "Top DPS notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner gets the most damage in a game";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants) {
        List<MatchParticipant> participants = match.getParticipants();
        MatchParticipant maxDamageDealt = Collections.max(participants,
                Comparator.comparing(
                        MatchParticipant::getTotalDamageDealtToChampions));
        
        for (Summoner summoner : trackedParticipants.keySet()) {
            if (trackedParticipants.get(summoner).getPuuid().equals(maxDamageDealt.getPuuid())) {
                TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
                result.setNotification(this);
                result.setMatch(match);
                result.setTitle("Top DPS!");
                result.addSubject(summoner, maxDamageDealt);
                
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }
}
