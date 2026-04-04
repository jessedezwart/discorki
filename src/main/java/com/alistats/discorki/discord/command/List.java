package com.alistats.discorki.discord.command;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class List extends AbstractCommand implements Command {

    @Override
    public String getCommandName() {
        return "list";
    }

    @Transactional(readOnly=true)
    public void run(SlashCommandInteractionEvent event) {
        Server server = obtainServer(event.getGuild());
        
        Hibernate.initialize(server.getPlayers());

        if (server.getPlayers().isEmpty()) {
            reply(event, "No summoners are registered. Use /add to add a summoner.");
            return;
        }

        LOG.debug("Listing all summoners in server {}", server.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        // for each player in server
        for (Player player : server.getPlayers()) {
            if (player.getSummoners().isEmpty()) {
                continue;
            }

            sb.append(player.getDiscordUsername())
                    .append("\n");
            // for each summoner in user
            for (Summoner summoner : player.getSummoners()) {
                sb.append("   - ")
                        .append(summoner.getRiotId())
                        .append("\n");
            }
        }

        reply(event, sb.toString());
    }
}
