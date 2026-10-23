package com.alistats.discorki.notification.game_start;

import java.util.Optional;
import java.util.Set;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.result.GameStartNotificationResult;

import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;

public interface GameStartNotification {
    Optional<GameStartNotificationResult> check(SpectatorGameInfo currentGame, Set<Summoner> summoners);
}
