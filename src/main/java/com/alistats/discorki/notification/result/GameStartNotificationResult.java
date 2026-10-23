package com.alistats.discorki.notification.result;

import java.util.HashMap;

import com.alistats.discorki.model.Summoner;

import lombok.Getter;
import lombok.Setter;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorParticipant;

@Getter
@Setter
public class GameStartNotificationResult extends Result {
    private HashMap<Summoner, SpectatorParticipant> subjects = new HashMap<>();
    private SpectatorGameInfo match;

    public void addSubject(Summoner summoner, SpectatorParticipant subject) {
        subjects.put(summoner, subject);
    }

    public void addSubject(HashMap<Summoner, SpectatorParticipant> subjects) {
        this.subjects.putAll(subjects);
    }
}