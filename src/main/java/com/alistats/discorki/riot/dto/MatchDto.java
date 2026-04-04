package com.alistats.discorki.riot.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    MetadataDto metadata;
    InfoDto info;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataDto {
        String dataVersion;
        String matchId;
        String[] participants;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoDto {
        String endOfGameResult;
        long gameCreation;
        long gameDuration;
        long gameEndTimestamp;
        long gameId;
        String gameMode;
        String gameName;
        String[] gameModeMutators;
        long gameStartTimestamp;
        String gameType;
        String gameVersion;
        int mapId;
        ParticipantDto[] participants;
        String platformId;
        int queueId;
        TeamDto[] teams;
        String tournamentCode;

        final int REMAKE_THRESHOLD_SECONDS_SR = 180;
        final int REMAKE_THRESHOLD_SECONDS_ARAM = 90;
        final int SUMMONERS_RIFT_MAP_ID = 1;

        public boolean isAborted() {
            if (this.getMapId() == SUMMONERS_RIFT_MAP_ID) {
                return this.getGameDuration() < REMAKE_THRESHOLD_SECONDS_SR;
            }
            return this.getGameDuration() < REMAKE_THRESHOLD_SECONDS_ARAM;
        }

        public boolean isRanked() {
            return queueId == 420 || queueId == 440;
        }

        public List<List<ParticipantDto>> getTeamCategorizedParticipants() {
            List<List<ParticipantDto>> teams = new ArrayList<>();
            teams.add(new ArrayList<>());
            teams.add(new ArrayList<>());

            for (ParticipantDto participant : participants) {
                if (participant.getTeamId() == 100) {
                    teams.get(0).add(participant);
                } else {
                    teams.get(1).add(participant);
                }
            }

            return teams;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ParticipantDto {
            int allInPings;
            int assistMePings;
            int assists;
            int baronKills;
            int basicPings;
            int baitPings;
            int bountyLevel;
            int champExperience;
            int champLevel;
            int championId;
            String championName;
            int championSkinId;
            int championTransform;
            Map<String, Object> challenges;
            int commandPings;
            int consumablesPurchased;
            int damageDealtToBuildings;
            int damageDealtToEpicMonsters;
            int damageDealtToObjectives;
            int damageDealtToTurrets;
            int damageSelfMitigated;
            int dangerPings;
            int deaths;
            int detectorWardsPlaced;
            int doubleKills;
            int dragonKills;
            boolean eligibleForProgression;
            int enemyMissingPings;
            int enemyVisionPings;
            boolean firstBloodAssist;
            boolean firstBloodKill;
            boolean firstTowerAssist;
            boolean firstTowerKill;
            boolean gameEndedInEarlySurrender;
            boolean gameEndedInSurrender;
            int getBackPings;
            int goldEarned;
            int goldSpent;
            int holdPings;
            String individualPosition;
            int inhibitorKills;
            int inhibitorTakedowns;
            int inhibitorsLost;
            int item0;
            int item1;
            int item2;
            int item3;
            int item4;
            int item5;
            int item6;
            int itemsPurchased;
            int killingSprees;
            int kills;
            String lane;
            int largestCriticalStrike;
            int largestKillingSpree;
            int largestMultiKill;
            int longestTimeSpentLiving;
            int magicDamageDealt;
            int magicDamageDealtToChampions;
            int magicDamageTaken;
            Map<String, Object> missions;
            int neutralMinionsKilled;
            int needVisionPings;
            int nexusKills;
            int nexusLost;
            int nexusTakedowns;
            int objectivesStolen;
            int objectivesStolenAssists;
            int onMyWayPings;
            int participantId;
            int pentaKills;
            PerksDto perks;
            int physicalDamageDealt;
            int physicalDamageDealtToChampions;
            int physicalDamageTaken;
            int placement;
            int playerAugment1;
            int playerAugment2;
            int playerAugment3;
            int playerAugment4;
            int playerAugment5;
            int playerAugment6;
            @JsonProperty("PlayerBehavior")
            Map<String, Object> playerBehavior;
            int playerSubteamId;
            int profileIcon;
            int pushPings;
            String puuid;
            int quadraKills;
            String riotIdGameName;
            String riotIdName;
            String riotIdTagline;
            int retreatPings;
            int roleBoundItem;
            String role;
            int sightWardsBoughtInGame;
            int spell1Casts;
            int spell2Casts;
            int spell3Casts;
            int spell4Casts;
            int subteamPlacement;
            int summoner1Casts;
            int summoner1Id;
            int summoner2Casts;
            int summoner2Id;
            String summonerId;
            int summonerLevel;
            String summonerName;
            boolean teamEarlySurrendered;
            int teamId;
            String teamPosition;
            int timeCCingOthers;
            int timePlayed;
            int totalAllyJungleMinionsKilled;
            int totalDamageDealt;
            int totalDamageDealtToChampions;
            int totalDamageShieldedOnTeammates;
            int totalDamageTaken;
            int totalEnemyJungleMinionsKilled;
            int totalHeal;
            int totalHealsOnTeammates;
            int totalMinionsKilled;
            int totalTimeCCDealt;
            int totalTimeSpentDead;
            int totalUnitsHealed;
            int tripleKills;
            int trueDamageDealt;
            int trueDamageDealtToChampions;
            int trueDamageTaken;
            int turretKills;
            int turretTakedowns;
            int turretsLost;
            int unrealKills;
            int visionClearedPings;
            int visionScore;
            int visionWardsBoughtInGame;
            int wardsKilled;
            int wardsPlaced;
            boolean win;

            public String getRiotId() {
                String gameName = StringUtils.hasText(riotIdGameName) ? riotIdGameName : riotIdName;
                if (StringUtils.hasText(gameName) && StringUtils.hasText(riotIdTagline)) {
                    return gameName + "#" + riotIdTagline;
                }
                return null;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PerksDto {
                PerkStatsDto statPerks;
                PerkStyleDto[] styles;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class PerkStatsDto {
                    int defense;
                    int flex;
                    int offense;
                }

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class PerkStyleDto {
                    String description;
                    PerkStyleSelectionDto[] selections;
                    int style;

                    @Data
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class PerkStyleSelectionDto {
                        int perk;
                        int var1;
                        int var2;
                        int var3;
                    }
                }
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TeamDto {
            BanDto[] bans;
            Map<String, Object> feats;
            ObjectivesDto objectives;
            int teamId;
            boolean win;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class BanDto {
                int championId;
                int pickTurn;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ObjectivesDto {
                ObjectiveDto atakhan;
                ObjectiveDto baron;
                ObjectiveDto champion;
                ObjectiveDto dragon;
                ObjectiveDto horde;
                ObjectiveDto inhibitor;
                ObjectiveDto riftHerald;
                ObjectiveDto tower;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class ObjectiveDto {
                    boolean first;
                    int kills;
                }
            }
        }
    }
}
