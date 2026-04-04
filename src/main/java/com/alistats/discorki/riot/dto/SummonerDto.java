package com.alistats.discorki.riot.dto;

import com.alistats.discorki.model.Summoner;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@Setter
public class SummonerDto {
    private int profileIconId;
    private long revisionDate;
    private String id;
    private String puuid;
    private long summonerLevel;
    // Set externally from AccountDto after a by-puuid lookup
    private String gameName;
    private String tagLine;

    public Summoner toSummoner() {
        Summoner summoner = new Summoner();
        // Keep the current entity primary key populated until the model is migrated off accountId.
        summoner.setAccountId(puuid);
        summoner.setProfileIconId(profileIconId);
        summoner.setRevisionDate(revisionDate);
        summoner.setName(gameName);
        summoner.setId(id);
        summoner.setPuuid(puuid);
        summoner.setLevel(summonerLevel);
        summoner.setGameName(gameName);
        summoner.setTagLine(tagLine);
        return summoner;
    }
}
