package com.alistats.discorki.discord.command;

import java.util.Optional;

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

@Component
public class Unlink extends AbstractCommand implements Command {
    @Override
    public String getCommandName() {
        return "unlink";
    }

    @SuppressWarnings("null")
    // Suppressed because the options are required, so it will never be null
    @Transactional
    public void run(SlashCommandInteractionEvent event) {
        User user = event.getOption("discord-username").getAsUser();
        String riotIdInput = event.getOption("riot-id").getAsString();

        // unlink a summoner from a user
        Server server = obtainServer(event.getGuild());
        Optional<Player> playerOpt = playerRepo.findByDiscordIdAndServer(user.getId(), server);
        if (playerOpt.isEmpty()) {
            reply(event, "User not found.");
            return;
        }
        Player player = playerOpt.get();

        Optional<Summoner> summonerOpt = RiotId.parse(riotIdInput)
                .flatMap(riotId -> player.getSummoners().stream()
                        .filter(summoner -> summoner.hasRiotId(riotId.gameName(), riotId.tagLine()))
                        .findFirst());
        if (summonerOpt.isEmpty()) {
            reply(event, String.format("***%s*** is not linked to <@%s>.", riotIdInput, user.getId()));
            return;
        }
        Summoner summoner = summonerOpt.get();

        player.removeSummoner(summoner);
        summoner.removeLinkedPlayer(player);
        playerRepo.save(player);
        reply(event, String.format("Unlinked ***%s*** from <@%s>.", summoner.getName(), user.getId()));
    }
}
