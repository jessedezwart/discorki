package com.alistats.discorki.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.service.ImageService;

public abstract class Notification {
    protected final LeagueApiController leagueApiController;
    protected final SummonerRepo summonerRepo;
    protected final ImageService imageService;
    protected final GameConstantsController leagueGameConstantsController;

    final protected Logger LOG = LoggerFactory.getLogger(Notification.class);

    protected Notification(LeagueApiController leagueApiController, SummonerRepo summonerRepo, ImageService imageService,
            GameConstantsController leagueGameConstantsController) {
        this.leagueApiController = leagueApiController;
        this.summonerRepo = summonerRepo;
        this.imageService = imageService;
        this.leagueGameConstantsController = leagueGameConstantsController;
    }

    public abstract String getName();
    public abstract String getFancyName();
    public abstract String getDescription();
}
