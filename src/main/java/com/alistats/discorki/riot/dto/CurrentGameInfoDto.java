package com.alistats.discorki.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentGameInfoDto {
    long gameId;
    String gameType;
    long gameStartTime;
    long mapId;
    long gameLength;
    String platformId;
    String gameMode;
    BannedChampionsDto[] bannedChampions;
    long gameQueueConfigId;
    ObserverDto observers;
    ParticipantDto[] participants;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannedChampionsDto {
        long championId;
        long teamId;
        int pickTurn;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObserverDto {
        String encryptionKey;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantDto {
        long championId;
        PerksDto perks;
        long profileIconId;
        boolean bot;
        long teamId;
        /** Legacy spectator field retained for compatibility with older responses/callers. */
        String summonerName;
        /** Legacy spectator field retained for compatibility with older responses/callers. */
        String summonerId;
        String puuid;
        String riotId;
        long spell1Id;
        long spell2Id;
        GameCustomizationObjectDto[] gameCustomizationObjects;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PerksDto {
            long perkStyle;
            long perkSubStyle;
            long[] perkIds;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GameCustomizationObjectDto {
            String category;
            String content;
        }
    }
}
