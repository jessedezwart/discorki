package com.alistats.discorki.discord.command;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.PlayerRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;
import com.alistats.discorki.service.TemplatingService;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Channel extends AbstractCommand implements Command {
    public Channel(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
    }

    @Override
    public String getCommandName() {
        return "channel";
    }

    public void run(SlashCommandInteractionEvent event) {
        long defaultChannelId = Optional.ofNullable(event.getOption("channel")).orElseThrow(() -> new RuntimeException("Channel cannot be empty.")).getAsLong();

        Server server = obtainServer(event.getGuild());
        server.setDefaultChannelId(defaultChannelId);
        serverRepo.save(server);
        
        reply(event, String.format("Default channel set to <#%s>.", defaultChannelId));
    }
}
