package com.alistats.discorki.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alistats.discorki.discord.EmbedFactory;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.LeagueApiController;

@Component
public abstract class Task {
    protected final LeagueApiController leagueApiController;
    protected final SummonerRepo summonerRepo;
    protected final MatchRepo matchRepo;
    protected final ServerRepo serverRepo;
    protected final EmbedFactory embedFactory;

    final Logger LOG = LoggerFactory.getLogger(Task.class);

    protected Task(LeagueApiController leagueApiController, SummonerRepo summonerRepo, MatchRepo matchRepo,
            ServerRepo serverRepo, EmbedFactory embedFactory) {
        this.leagueApiController = leagueApiController;
        this.summonerRepo = summonerRepo;
        this.matchRepo = matchRepo;
        this.serverRepo = serverRepo;
        this.embedFactory = embedFactory;
    }
}
