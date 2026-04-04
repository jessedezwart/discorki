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
public class Remove extends AbstractCommand implements Command {
    public Remove(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
    }

    @Override
    public String getCommandName() {
        return "remove";
    }

    @SuppressWarnings("null")
    // Suppressed because the option discord=username is required, so it will never be null
    public void run(SlashCommandInteractionEvent event) {
        String userId = event.getOption("discord-username").getAsUser().getId();
        playerRepo.deleteByDiscordIdAndServer(userId, obtainServer(event.getGuild()));
        event.getHook().sendMessage(String.format("Stopped tracking <@%s>", userId)).queue();
    }
}
