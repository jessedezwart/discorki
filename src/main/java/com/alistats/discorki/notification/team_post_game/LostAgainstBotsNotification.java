package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchTeam;

// Check if summoner lost custom or coop vs AI
@Component
public class LostAgainstBotsNotification extends Notification implements TeamPostGameNotification {
    @Override
    public String getName() {
        return "LostAgainstBotsNotification";
    }

    @Override
    public String getFancyName() {
        return "Lost against bots notification";
    }

    @Override
    public String getDescription() {
        return "Notifies when a summoner lost against bots.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(LOLMatch match,
            HashMap<Summoner, MatchParticipant> trackedParticipants) {
        if (didAFullBotTeamWin(match)) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(trackedParticipants);
            result.setTitle("Lost against bots");

            return Optional.of(result);
        }

        return Optional.empty();
    }

    private boolean didAFullBotTeamWin(LOLMatch match) {
        List<MatchTeam> teams = match.getTeams();
        return teams.stream()
                .filter(MatchTeam::didWin)
                .anyMatch(
                        team -> {
                            List<MatchParticipant> participants = match.getParticipants();

                            // Assuming here that an empty team qualifies as a "full bot team"
                            return participants.stream()
                                    .filter(p -> p.getTeam() == team.getTeamId())
                                    .allMatch(p -> p.getPuuid().equals("BOT"));
                        }

                );
    }
}
