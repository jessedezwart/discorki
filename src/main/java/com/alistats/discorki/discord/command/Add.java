package com.alistats.discorki.discord.command;

import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.PlayerRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.dto.SummonerDto;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;
import com.alistats.discorki.service.TemplatingService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Add extends AbstractCommand implements Command {
    public Add(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
    }

    @Override
    public String getCommandName() {
        return "add";
    }

    @Transactional(readOnly = false)
    public void run(SlashCommandInteractionEvent event) throws Exception {
        // Get and validate options
        User jdaUser = Optional.ofNullable(event.getOption("discord-username"))
                .orElseThrow(() -> new RuntimeException("User cannot be empty.")).getAsUser();
        String riotIdInput = Optional.ofNullable(event.getOption("riot-id"))
                .orElseThrow(() -> new RuntimeException("Riot ID cannot be empty.")).getAsString();

        // Parse "gameName#tagLine"
        String[] parts = riotIdInput.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            reply(event, "Invalid Riot ID format. Use `GameName#TAG` (e.g. `PlayerName#EUW`).");
            return;
        }
        String gameName = parts[0].trim();
        String tagLine = parts[1].trim();

        // Check if user is not a bot
        if (jdaUser.isBot()) {
            LOG.info("User {} tried to link a bot.", event.getUser().getName());
            event.getHook().sendMessage("Cannot link a bot.").queue();
            return;
        }

        // Get or create server and player
        Server server = obtainServer(event.getGuild());

        LOG.debug("Looking for player named {} and server {}", jdaUser.getName(), server.getName());
        Player player = playerRepo.findByDiscordIdAndServer(jdaUser.getId(), server)
                .orElseGet(() -> {
                    Player newPlayer = new Player(jdaUser, server);
                    playerRepo.save(newPlayer);
                    return newPlayer;
                });

        Hibernate.initialize(player.getSummoners());

        // Check if summoner was already linked
        if (player.hasSummonerByRiotId(gameName, tagLine)) {
            LOG.info("User {} tried to link {}#{} to {} but it was already linked.",
                    event.getUser().getName(), gameName, tagLine, jdaUser.getName());
            reply(event, String.format("Summoner ***%s#%s*** is already linked to <@%s>", gameName, tagLine, jdaUser.getId()));
            return;
        }

        // Look up or create summoner via Riot Account API → Summoner API
        Summoner summoner = summonerRepo.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .orElseGet(() -> {
                    try {
                        var accountDto = leagueApiController.getAccountByRiotId(gameName, tagLine);
                        SummonerDto summonerDto = leagueApiController.getSummonerByPuuid(accountDto.getPuuid());
                        summonerDto.setGameName(accountDto.getGameName());
                        summonerDto.setTagLine(accountDto.getTagLine());
                        return summonerRepo.save(summonerDto.toSummoner());
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                            reply(event, String.format("Summoner ***%s#%s*** not found.", gameName, tagLine));
                        } else {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                });

        if (summoner == null) {
            return;
        }

        // Fetch rank
        LOG.debug("Fetching rank for summoner {}#{}", gameName, tagLine);
        try {
            rankService.updateRanks(summoner);
            player.addSummoner(summoner);
            playerRepo.save(player);
            reply(event, String.format("Linked ***%s#%s*** to <@%s>.", gameName, tagLine, jdaUser.getId()));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                reply(event, String.format("Summoner ***%s#%s*** not found.", gameName, tagLine));
            } else {
                throw new Exception(e);
            }
        }
    }
}
