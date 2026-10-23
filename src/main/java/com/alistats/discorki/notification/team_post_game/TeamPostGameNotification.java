package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
import java.util.Optional;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@FunctionalInterface
public interface TeamPostGameNotification {
    Optional<TeamPostGameNotificationResult> check(LOLMatch match, HashMap<Summoner, MatchParticipant> trackedParticipants);
}