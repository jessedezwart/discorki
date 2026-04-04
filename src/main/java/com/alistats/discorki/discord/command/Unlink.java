package com.alistats.discorki.discord.command;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.Summoner;
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

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class Unlink extends AbstractCommand implements Command {
    public Unlink(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
    }

    @Override
    public String getCommandName() {
        return "unlink";
    }
    
    @SuppressWarnings("null")
    // Suppressed because the options are required, so it will never be null
    public void run(SlashCommandInteractionEvent event) {
        String discordId = event.getOption("discord-username").getAsUser().getId();
        Optional<Player> userOpt = playerRepo.findByDiscordIdAndServer(discordId, obtainServer(event.getGuild()));
        if (userOpt.isEmpty()) {
            event.getHook().sendMessage("User not found.").queue();
            return;
        }

        String riotIdInput = event.getOption("riot-id").getAsString();
        String[] parts = riotIdInput.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            event.getHook().sendMessage("Invalid Riot ID format. Use `GameName#TAG` (e.g. `PlayerName#EUW`).").queue();
            return;
        }

        Optional<Summoner> summonerOpt = summonerRepo.findByGameNameIgnoreCaseAndTagLineIgnoreCase(
                parts[0].trim(), parts[1].trim());
        if (summonerOpt.isEmpty()) {
            event.getHook().sendMessage("Summoner not found.").queue();
            return;
        }
        Player player = userOpt.get();
        Summoner summoner = summonerOpt.get();

        player.removeSummonerById(summoner.getId());
        summoner.removeLinkedPlayerById(player.getId());
        playerRepo.save(player);
        summonerRepo.save(summoner);
        event.getHook().sendMessage(String.format("Unlinked ***%s*** from <@%s>.", summoner.getRiotId(), player.getDiscordId()))
                .queue();
    }
}
