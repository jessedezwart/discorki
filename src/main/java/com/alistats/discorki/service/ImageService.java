package com.alistats.discorki.service;

import java.net.URI;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.model.Tier;
import com.alistats.discorki.riot.controller.GameConstantsController;

import no.stelar7.api.r4j.pojo.lol.staticdata.champion.StaticChampion;

@Service
public class ImageService {
    @Autowired
    private CustomConfigProperties customConfig;
    @Autowired
    private GameConstantsController gameConstantsController;

    private static final String DATA_DRAGON_URL = "https://ddragon.leagueoflegends.com/cdn";

    public URL getChampionTileUrl(int championId) {
        String image = gameConstantsController.getChampion(championId)
                .map(StaticChampion::getImage)
                .map(img -> img.getFull())
                .orElse("-1.png");

        return resolveUrl(DATA_DRAGON_URL + "/" + gameConstantsController.getLatestVersion() + "/img/champion/" + image);
    }

    public URL getMapUrl(int mapId) {
        return resolveUrl(DATA_DRAGON_URL + "/" + gameConstantsController.getLatestVersion() + "/img/map/map" + mapId
                + ".png");
    }

    public URL getRankEmblemUrl(Tier tier) {
        String tierStr = tier.toString().toLowerCase();

        return resolveUrl("https://" + customConfig.getHost() + "/rank_emblems/" + tierStr + ".png");
    }

    private URL resolveUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
