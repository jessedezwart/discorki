package com.alistats.discorki.discord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alistats.discorki.model.Server;
import com.alistats.discorki.repository.ServerRepo;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Service
public class EventListener extends ListenerAdapter {
    @Autowired
    private ServerRepo serverRepo;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild jdaGuild = event.getGuild();
        DefaultGuildChannelUnion defaultChannel = jdaGuild.getDefaultChannel();

        // Add guild to database, or reactivate it when the bot is re-added
        Server server = serverRepo.findById(jdaGuild.getId()).orElseGet(() -> {
            Server newServer = new Server();
            newServer.setId(jdaGuild.getId());
            return newServer;
        });
        server.setName(jdaGuild.getName());
        server.setActive(true);
        if (server.getDefaultChannelId() == 0 && defaultChannel != null) {
            server.setDefaultChannelId(defaultChannel.getIdLong());
        }
        serverRepo.save(server);

        if (defaultChannel != null) {
            defaultChannel.asStandardGuildMessageChannel().sendMessage(
                    "Hello, I am Discorki! I track achievements for League of Legends players. Add a player with /add!")
                    .queue();
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        serverRepo.findById(event.getGuild().getId()).ifPresent(server -> {
            server.setActive(false);
            serverRepo.save(server);
        });
    }
}
