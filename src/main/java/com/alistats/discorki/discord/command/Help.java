package com.alistats.discorki.discord.command;

import org.springframework.stereotype.Component;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
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
public class Help extends AbstractCommand implements Command {
    public Help(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
    }

    @Override
    public String getCommandName() {
        return "help";
    }

    public void run(SlashCommandInteractionEvent event) throws Exception {
        String helpText = templatingService.renderTemplate("templates/DiscordHelpCommand.pebble", null);
        reply(event, helpText);
    }
}
