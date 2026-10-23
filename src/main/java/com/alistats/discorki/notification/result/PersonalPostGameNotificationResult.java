package com.alistats.discorki.notification.result;

import com.alistats.discorki.model.Summoner;

import lombok.Getter;
import lombok.Setter;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;

@Getter
@Setter
public class PersonalPostGameNotificationResult extends Result {
    private Summoner subject;
    private LOLMatch match;
}