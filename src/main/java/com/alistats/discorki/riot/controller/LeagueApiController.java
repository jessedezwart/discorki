package com.alistats.discorki.riot.controller;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.alistats.discorki.config.RiotConfigProperties;

import no.stelar7.api.r4j.basic.APICredentials;
import no.stelar7.api.r4j.basic.calling.DataCall;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.constants.api.regions.RegionShard;
import no.stelar7.api.r4j.basic.constants.types.ApiKeyType;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.R4J;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;
import no.stelar7.api.r4j.pojo.shared.RiotAccount;

/**
 * Thin wrapper around R4J that binds every call to the configured platform and
 * region. R4J takes care of rate limiting (based on the headers Riot sends back)
 * and of retrying 429 and 5xx responses.
 *
 * Unless stated otherwise, methods return null when Riot responds with a 404.
 */
@Service
public class LeagueApiController {
        private static final Logger LOG = LoggerFactory.getLogger(LeagueApiController.class);

        private final R4J r4j;
        private final LeagueShard platform;
        private final RegionShard region;
        // account-v1 is not served from the SEA region, those accounts live in ASIA
        private final RegionShard accountRegion;

        public LeagueApiController(RiotConfigProperties config) {
                r4j = new R4J(new APICredentials(config.getKey()));
                // R4J installs a file system cache by default, which would serve stale
                // spectator and league data. Caching is done with Spring where needed.
                DataCall.setCacheProvider(null);

                platform = LeagueShard.fromString(config.getPlatformRouting())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Unknown platform routing: " + config.getPlatformRouting()));
                region = RegionShard.getFromCode(config.getRegionalRouting())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Unknown regional routing: " + config.getRegionalRouting()));
                accountRegion = region == RegionShard.SEA ? RegionShard.ASIA : region;
        }

        public RiotAccount getAccountByRiotId(String gameName, String tagLine) {
                return call(() -> r4j.getAccountAPI().getAccountByTag(accountRegion, gameName, tagLine, ApiKeyType.LOL));
        }

        public RiotAccount getAccountByPuuid(String puuid) {
                return call(() -> r4j.getAccountAPI().getAccountByPUUID(accountRegion, puuid, ApiKeyType.LOL));
        }

        public Summoner getSummoner(String puuid) {
                return call(() -> r4j.getLoLAPI().getSummonerAPI().getSummonerByPUUID(platform, puuid));
        }

        /**
         * @return the game the player is currently in, or null if they are not in a game
         */
        public SpectatorGameInfo getCurrentGame(String puuid) {
                return call(() -> r4j.getLoLAPI().getSpectatorAPI().getCurrentGame(platform, puuid));
        }

        /**
         * @param gameId the game id without platform prefix, as returned by the spectator API
         * @return the match, or null if it has not finished yet
         */
        public LOLMatch getMatch(long gameId) {
                return getMatch(platform.name() + "_" + gameId);
        }

        /**
         * @param matchId the match id with platform prefix, e.g. EUW1_1234567890
         */
        public LOLMatch getMatch(String matchId) {
                return call(() -> r4j.getLoLAPI().getMatchAPI().getMatch(region, matchId));
        }

        /**
         * @return all league entries of the player, empty when unranked
         */
        @Cacheable("ranks")
        public List<LeagueEntry> getLeagueEntries(String puuid) {
                return call(() -> r4j.getLoLAPI().getLeagueAPI().getLeagueEntriesByPUUID(platform, puuid));
        }

        public String getMostRecentMatchId(String puuid) {
                List<String> matchIds = call(() -> r4j.getLoLAPI().getMatchAPI()
                                .getMatchList(region, puuid, null, null, 0, 1, null, null));
                if (matchIds == null || matchIds.isEmpty()) {
                        return null;
                }
                return matchIds.get(0);
        }

        public LeagueShard getPlatform() {
                return platform;
        }

        private <T> T call(Supplier<T> supplier) {
                try {
                        return supplier.get();
                } catch (APIResponseException e) {
                        if (e.getReason() == APIHTTPErrorReason.ERROR_401
                                        || e.getReason() == APIHTTPErrorReason.ERROR_403) {
                                LOG.error("Riot API key was rejected, did it expire?");
                        }
                        throw e;
                }
        }
}
