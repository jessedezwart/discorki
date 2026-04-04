package com.alistats.discorki.discord.command;

import org.springframework.stereotype.Component;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Remove extends AbstractCommand implements Command {
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
