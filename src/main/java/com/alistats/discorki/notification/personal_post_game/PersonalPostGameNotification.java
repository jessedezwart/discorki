package com.alistats.discorki.notification.personal_post_game;

import java.util.Optional;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;

public interface PersonalPostGameNotification {
    Optional<PersonalPostGameNotificationResult> check(LOLMatch match, Summoner summoner);
}