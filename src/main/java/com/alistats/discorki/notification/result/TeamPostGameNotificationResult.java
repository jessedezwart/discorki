package com.alistats.discorki.notification.result;

import java.util.HashMap;

import com.alistats.discorki.model.Summoner;

import lombok.Getter;
import lombok.Setter;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Getter
@Setter
public class TeamPostGameNotificationResult extends Result {
    private HashMap<Summoner, MatchParticipant> subjects = new HashMap<>();
    private LOLMatch match;

    public void addSubject(Summoner summoner, MatchParticipant subject) {
        subjects.put(summoner, subject);
    }
}
