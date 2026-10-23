package com.alistats.discorki.notification.game_start;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.GameStartNotificationResult;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorParticipant;

@Component
public class ClashGameStartNotification extends Notification implements GameStartNotification {
    @Override
    public String getName() {
        return "ClashGameStartNotification";
    }
    @Override
    public String getFancyName() {
        return "Clash game start notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a clash game starts.";
    }

    public Optional<GameStartNotificationResult> check(SpectatorGameInfo currentGame, Set<Summoner> summoners) {
        // Check if the game is a clash game
        if (currentGame.getGameQueueConfig() != GameQueueType.CLASH) {
            return Optional.empty();
        }

        // Find summoner in participants
        List<SpectatorParticipant> participants = currentGame.getParticipants();

        HashMap<Summoner, SpectatorParticipant> summonersInGame = new HashMap<>();
        
        // Check for tracked summoners if they are in the current game
        for (Summoner summoner : summoners) {
            for (SpectatorParticipant participant : participants) {
                if (summoner.getPuuid().equals(participant.getPuuid())) {
                    summonersInGame.put(summoner, participant);
                }
            }
        }

        if (summonersInGame.size() > 0) {
            GameStartNotificationResult result = new GameStartNotificationResult();
            result.setNotification(this);
            result.addSubject(summonersInGame);
            result.setMatch(currentGame);

            return Optional.of(result);
        }
        
        return Optional.empty();
    }
}
