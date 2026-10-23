package com.alistats.discorki.discord.command;

import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.riot.RiotId;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import no.stelar7.api.r4j.pojo.shared.RiotAccount;

@Component
public class Add extends AbstractCommand implements Command {

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
        Optional<RiotId> riotIdOpt = RiotId.parse(riotIdInput);
        if (riotIdOpt.isEmpty()) {
            reply(event, String.format("***%s*** is not a valid Riot ID. Use the format `name#tag`.", riotIdInput));
            return;
        }
        RiotId riotId = riotIdOpt.get();

        // Check if user is not a bot
        if (jdaUser.isBot()) {
            LOG.info("User {} tried to link a bot.", event.getUser().getName());
            event.getHook().sendMessage("Cannot link a bot.").queue();
            return;
        }

        // Get or create server and player
        Server server = obtainServer(event.getGuild());

        // Check if there already is a player with the same discord id and that guild
        LOG.debug("Looking for player named {} and server {}", jdaUser.getName(), server.getName());
        Player player = playerRepo.findByDiscordIdAndServer(jdaUser.getId(), server)
                .orElseGet(() -> playerRepo.save(new Player(jdaUser, server)));

        Hibernate.initialize(player.getSummoners());

        // Look up the account, Riot IDs can change so always ask Riot for the puuid
        RiotAccount account = leagueApiController.getAccountByRiotId(riotId.gameName(), riotId.tagLine());
        if (account == null) {
            reply(event, String.format("Riot ID ***%s*** not found.", riotId));
            return;
        }

        Summoner summoner = summonerRepo.findById(account.getPUUID()).orElseGet(Summoner::new);

        // Check if summoner was already linked
        if (player.hasSummoner(summoner)) {
            LOG.info("User {} tried to link summoner {} to {} but it was already linked.",
                    event.getUser().getName(), riotId, jdaUser.getName());
            reply(event, String.format("***%s*** is already linked to <@%s>", riotId, jdaUser.getId()));
            return;
        }

        no.stelar7.api.r4j.pojo.lol.summoner.Summoner summonerDto = leagueApiController.getSummoner(account.getPUUID());
        if (summonerDto == null) {
            reply(event, String.format("***%s*** has never played League of Legends on this server.", riotId));
            return;
        }

        summoner.setPuuid(account.getPUUID());
        summoner.setGameName(account.getName());
        summoner.setTagLine(account.getTag());
        summoner.setProfileIconId(summonerDto.getProfileIconId());
        summoner.setRevisionDate(summonerDto.getRevisionDate());
        summoner.setLevel(summonerDto.getSummonerLevel());
        summoner = summonerRepo.save(summoner);

        // Fetch rank
        LOG.debug("Fetching rank for summoner {}", riotId);
        rankService.updateRanks(summoner);

        // Add summoner to user
        player.addSummoner(summoner);
        playerRepo.save(player);
        reply(event, String.format("Linked %s to <@%s>.", summoner.getName(), jdaUser.getId()));
    }
}
