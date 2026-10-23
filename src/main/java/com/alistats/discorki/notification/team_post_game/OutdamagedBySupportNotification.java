package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class OutdamagedBySupportNotification extends Notification implements TeamPostGameNotification {
    @Override
    public String getName() {
        return "OutdamagedBySupportNotification";
    }
    @Override
    public String getFancyName() {
        return "Outdamaged by support notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when you are outdamaged by your support.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants) {
        // Since we COULD potentially have tracked players on opposite teams:
        // one being support on red, one being adc on blue, we SHOULD check
        // on team-basis. But this is extremely unlikely. For now, we can
        // just check on role and assume the tracked participants are on the same team.
        // After, we check team-equality, so we only have false negatives, no false
        // positives
        MatchParticipant bottom = null;
        MatchParticipant support = null;

        // Loop through tracked participants
        for (MatchParticipant participant : trackedParticipants.values()) {
            // Check if participant is adc
            if (participant.getChampionSelectLane() == LaneType.BOT) {
                bottom = participant;
            }
            // Check if participant is support
            else if (participant.getChampionSelectLane() == LaneType.UTILITY) {
                support = participant;
            }
        }

        // Support and bottom are not tracked, or they belong to different teams
        if (support == null || bottom == null || support.getTeam() != bottom.getTeam()) {
            return Optional.empty();
        }

        // Check if support did more damage than adc
        if (support.getTotalDamageDealtToChampions() > bottom.getTotalDamageDealtToChampions()) {
            for (Summoner summoner : trackedParticipants.keySet()) {
                if (trackedParticipants.get(summoner).getPuuid().equals(support.getPuuid())) {
                    TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
                    result.setNotification(this);
                    result.setMatch(match);
                    result.addSubject(summoner, support);
                    result.setTitle("Outdamaged by support!");
                    result.addExtraArgument("bottom", bottom);
                    result.addExtraArgument("support", support);

                    return Optional.of(result);
                }
            }
        }
        
        return Optional.empty();
    }
}
