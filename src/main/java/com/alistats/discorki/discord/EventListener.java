package com.alistats.discorki.discord;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.alistats.discorki.model.Server;
import com.alistats.discorki.repository.ServerRepo;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Service
public class EventListener extends ListenerAdapter {
    private final ServerRepo serverRepo;

    public EventListener(ServerRepo serverRepo) {
        this.serverRepo = serverRepo;
    }

    @Override
    @SuppressWarnings("null")
    public void onGuildJoin(GuildJoinEvent event) {
        Guild jdaGuild = event.getGuild();

        Optional<Server> serverOpt = serverRepo.findById(jdaGuild.getId());

        // Check if guild is in database
        if (serverOpt.isEmpty()) {
            // Add guild to database
            Server server = new Server();
            server.setId(jdaGuild.getId());
            server.setName(jdaGuild.getName());

            serverRepo.save(server);
        }

        if (jdaGuild.getDefaultChannel() == null) {
            throw new RuntimeException("Guild has no default channel");
        }
        jdaGuild.getDefaultChannel().asTextChannel().sendMessage(
                "Hello, I am Discorki! I track achievements for League of Legends players. Add a player with /add!")
                .queue();
    }

    @Override
    @SuppressWarnings("null")
    public void onGuildLeave(GuildLeaveEvent event) {
        Guild jdaGuild = event.getGuild();
        serverRepo.findById(jdaGuild.getId()).orElseThrow(() -> new RuntimeException("Guild not found"))
                .setActive(false);
    }
}
