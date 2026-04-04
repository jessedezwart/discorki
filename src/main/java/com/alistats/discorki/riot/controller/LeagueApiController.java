package com.alistats.discorki.riot.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.alistats.discorki.config.RestTemplateResponseErrorHandler;
import com.alistats.discorki.config.RiotConfigProperties;
import com.alistats.discorki.riot.dto.AccountDto;
import com.alistats.discorki.riot.dto.CurrentGameInfoDto;
import com.alistats.discorki.riot.dto.LeagueEntryDto;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.SummonerDto;
import com.google.common.util.concurrent.RateLimiter;

@Service
public class LeagueApiController {
        private final RiotConfigProperties config;
        private final RestTemplate restTemplate;
        private final RateLimiter rateLimiter;

        private static final String BASE_URL = "api.riotgames.com/lol";
        private static final String ACCOUNT_URL = "api.riotgames.com/riot/account/v1";
        private static final int TWO_MINUTES_IN_SECONDS = 120;

        // Since the checkInGame task is scheduled every 5 minutes at second 0
        // and the checkMatchFinished task is scheduled every minute at second 30,
        // we assume that the timeout wont interfere with new requests
        private static final int MAX_RATE_LIMIT_TIMEOUT = 30;

        public LeagueApiController(RiotConfigProperties config, RestTemplateBuilder restTemplateBuilder) {
                restTemplate = restTemplateBuilder
                                .errorHandler(new RestTemplateResponseErrorHandler())
                                .build();
                this.config = config;

                rateLimiter = RateLimiter
                                .create(Double.parseDouble(config.getRateLimitPerTwoMinutes())
                                                / TWO_MINUTES_IN_SECONDS);
        }

        private void rateLimit() {
                rateLimiter.tryAcquire(1, MAX_RATE_LIMIT_TIMEOUT, TimeUnit.SECONDS);
        }

        /**
         * Looks up a Riot account by Riot ID (gameName + tagLine) using the Account v1 API.
         * This replaces the deprecated summoner-by-name endpoint.
         */
        public AccountDto getAccountByRiotId(String gameName, String tagLine)
                        throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                String url = "https://" +
                                config.getRegionalRouting() +
                                "." +
                                ACCOUNT_URL +
                                "/accounts/by-riot-id/" +
                                URLEncoder.encode(gameName, StandardCharsets.UTF_8) +
                                "/" +
                                URLEncoder.encode(tagLine, StandardCharsets.UTF_8) +
                                "?api_key=" +
                                config.getKey();

                URI uri = URI.create(url);

                return restTemplate.getForObject(uri, AccountDto.class);
        }

        /**
         * Fetches summoner data by PUUID (summoner v4).
         * Use after {@link #getAccountByRiotId} to get the full summoner profile.
         */
        public SummonerDto getSummonerByPuuid(String encryptedPuuid)
                        throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                String url = "https://" +
                                config.getPlatformRouting() +
                                "." +
                                BASE_URL +
                                "/summoner/v4/summoners/by-puuid/" +
                                encryptedPuuid +
                                "?api_key=" +
                                config.getKey();

                URI uri = URI.create(url);

                return restTemplate.getForObject(uri, SummonerDto.class);
        }

        public CurrentGameInfoDto getCurrentGameInfo(String encryptedSummonerId)
                        throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                String url = "https://" +
                                config.getPlatformRouting() +
                                "." +
                                BASE_URL +
                                "/spectator/v5/active-games/by-summoner/" +
                                encryptedSummonerId +
                                "?api_key=" +
                                config.getKey();

                URI uri = URI.create(url);

                return restTemplate.getForObject(uri, CurrentGameInfoDto.class);
        }

        @Cacheable("matches")
        public MatchDto getMatch(long matchId) throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                String url = "https://" +
                                config.getRegionalRouting() +
                                "." +
                                BASE_URL +
                                "/match/v5/matches/" +
                                config.getPlatformRouting().toUpperCase() +
                                "_" +
                                matchId +
                                "?api_key=" +
                                config.getKey();

                URI uri = URI.create(url);

                return restTemplate.getForObject(uri, MatchDto.class);
        }

        @Cacheable("ranks")
        public LeagueEntryDto[] getLeagueEntriesByPuuid(String puuid)
                        throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                String encodedPuuid = URLEncoder.encode(puuid, StandardCharsets.UTF_8);
                String url = "https://" +
                                config.getPlatformRouting() +
                                "." +
                                BASE_URL +
                                "/league/v4/entries/by-puuid/" +
                                encodedPuuid +
                                "?api_key=" +
                                config.getKey();

                URI uri = URI.create(url);

                return restTemplate.getForObject(uri, LeagueEntryDto[].class);
        }

        public String getMostRecentMatchId(String puuid)
                        throws HttpClientErrorException, HttpServerErrorException {
                rateLimit();
                StringBuilder url = new StringBuilder();
                url.append("https://")
                                .append(config.getRegionalRouting())
                                .append(".")
                                .append(BASE_URL)
                                .append("/match/v5/matches/by-puuid/")
                                .append(puuid)
                                .append("/ids?start=0&count=1&api_key=")
                                .append(config.getKey());

                URI uri = URI.create(url.toString());
                String[] matchIds = restTemplate.getForObject(uri, String[].class);
                if (matchIds != null) {
                        return matchIds[0];
                }
                return null;
        }
}
