package com.alistats.discorki.riot.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import no.stelar7.api.r4j.impl.lol.raw.DDragonAPI;
import no.stelar7.api.r4j.pojo.lol.staticdata.champion.StaticChampion;

/**
 * Static game data from Data Dragon. Always uses the latest patch, so new
 * champions show up without a config change.
 */
@Service
public class GameConstantsController {
    private static final Logger LOG = LoggerFactory.getLogger(GameConstantsController.class);
    private static final long REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000;

    public enum ChampionClass {
        Assassin, Fighter, Mage, Marksman, Support, Tank
    }

    private volatile String version;
    private volatile Map<Integer, StaticChampion> champions;

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MS, initialDelay = REFRESH_INTERVAL_MS)
    public synchronized void refresh() {
        List<String> versions = DDragonAPI.getInstance().getVersions();
        if (versions.isEmpty()) {
            LOG.warn("Could not fetch Data Dragon versions, keeping {}", version);
            return;
        }
        String latest = versions.get(0);
        Map<Integer, StaticChampion> latestChampions = DDragonAPI.getInstance().getChampions(latest, null);
        if (latestChampions.isEmpty()) {
            LOG.warn("Could not fetch champions for Data Dragon {}, keeping {}", latest, version);
            return;
        }
        version = latest;
        champions = latestChampions;
        LOG.info("Loaded {} champions from Data Dragon {}", champions.size(), version);
    }

    public String getLatestVersion() {
        ensureLoaded();
        return version;
    }

    public Map<Integer, StaticChampion> getChampions() {
        ensureLoaded();
        return champions;
    }

    public Optional<StaticChampion> getChampion(int championId) {
        return Optional.ofNullable(getChampions().get(championId));
    }

    public Set<String> getChampionNames() {
        return getChampions().values().stream()
                .map(StaticChampion::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getChampionNamesByClass(ChampionClass championClass) {
        return getChampions().values().stream()
                .filter(champion -> champion.getTags().contains(championClass.name()))
                .map(StaticChampion::getName)
                .collect(Collectors.toSet());
    }

    public String getChampionName(int championId) {
        return getChampion(championId).map(StaticChampion::getName).orElse("Unknown champion");
    }

    private void ensureLoaded() {
        if (champions == null) {
            refresh();
        }
        if (champions == null) {
            throw new IllegalStateException("Data Dragon data is not available");
        }
    }
}
