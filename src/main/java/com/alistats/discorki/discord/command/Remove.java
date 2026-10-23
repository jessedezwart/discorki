package com.alistats.discorki.discord.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Server;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Remove extends AbstractCommand implements Command {
    @Override
    public String getCommandName() {
        return "remove";
    }

    @SuppressWarnings("null")
    // Suppressed because the option discord=username is required, so it will never be null
    @Transactional
    public void run(SlashCommandInteractionEvent event) {
        String userId = event.getOption("discord-username").getAsUser().getId();
        Server server = obtainServer(event.getGuild());
        playerRepo.findByDiscordIdAndServer(userId, server).ifPresent(playerRepo::delete);
        reply(event, String.format("Stopped tracking <@%s>", userId));
    }
}
