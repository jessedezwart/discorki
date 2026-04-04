package com.alistats.discorki.riot.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alistats.discorki.config.RiotConfigProperties;
import com.alistats.discorki.riot.dto.constants.ChampionDto;
import com.alistats.discorki.riot.dto.constants.GameModeDto;
import com.alistats.discorki.riot.dto.constants.GameTypeDto;
import com.alistats.discorki.riot.dto.constants.MapDto;
import com.alistats.discorki.riot.dto.constants.QueueDto;
import com.alistats.discorki.riot.dto.constants.SeasonDto;

@Service
public class GameConstantsController {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String BASE_URL_DOCS = "https://static.developer.riotgames.com/docs/lol";
    private final String BASE_URL_DDRAGON;

    public GameConstantsController(RiotConfigProperties riotConfig) {
        String version = fetchLatestDDragonVersion();
        BASE_URL_DDRAGON = "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/en_US";
    }

    private String fetchLatestDDragonVersion() {
        try {
            String[] versions = restTemplate.getForObject(
                    "https://ddragon.leagueoflegends.com/api/versions.json", String[].class);
            if (versions != null && versions.length > 0) {
                return versions[0];
            }
        } catch (Exception e) {
            // fall through to safe default
        }
        return "15.7.1";
    }

    @Cacheable("gamemodes")
    public GameModeDto[] getGameModes() {
        return restTemplate.getForObject(BASE_URL_DOCS + "/gameModes.json", GameModeDto[].class);
    }

    @Cacheable("gametypes")
    public GameTypeDto[] getGameTypes() {
        return restTemplate.getForObject(BASE_URL_DOCS + "/gameTypes.json", GameTypeDto[].class);
    }

    @Cacheable("maps")
    public MapDto[] getMaps() {
        return restTemplate.getForObject(BASE_URL_DOCS + "/maps.json", MapDto[].class);
    }

    @Cacheable("queues")
    public QueueDto[] getQueues() {
        return restTemplate.getForObject(BASE_URL_DOCS + "/queues.json", QueueDto[].class);
    }

    @Cacheable("seasons")
    public SeasonDto[] getSeasons() {
        return restTemplate.getForObject(BASE_URL_DOCS + "/seasons.json", SeasonDto[].class);
    }

    @Cacheable("champions")
    public ChampionDto getChampions() {
        return restTemplate.getForObject(BASE_URL_DDRAGON + "/champion.json", ChampionDto.class);
    }

    public QueueDto getQueue(int queueId) {
        QueueDto[] queues = getQueues();
        for (QueueDto queue : queues) {
            if (queue.getQueueId() == queueId) {
                return queue;
            }
        }
        return null;
    }

    
    public Set<String> getChampionNames() {
        return getChampions().getData().keySet();
    }

    public Set<String> getChampionNamesByClass(ChampionDto.Champion.Class className) {
        return getChampions().getData().entrySet().stream()
                .filter(entry -> entry.getValue().getTags().contains(className.toString()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }    

    public String getChampionNameByKey(int key) {
        Collection<ChampionDto.Champion> champions = getChampions().getData().values();
        for (ChampionDto.Champion champion : champions) {
            if (champion.getKey() == key) {
                return champion.getName();
            }
        }
        return null;
    }

    public String getChampionIdByKey(int key) {
        for (ChampionDto.Champion champion : getChampions().getData().values()) {
            if (champion.getKey() == key) {
                return champion.getId();
            }
        }
        return null;
    }
}
