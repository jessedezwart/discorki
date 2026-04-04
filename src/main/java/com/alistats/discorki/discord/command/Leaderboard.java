package com.alistats.discorki.discord.command;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Leaderboard extends AbstractCommand implements Command {
    @Override
    public String getCommandName() {
        return "leaderboard";
    }

    @SuppressWarnings("null")
    @Transactional()
    public void run(SlashCommandInteractionEvent event) {
        Server server = obtainServer(event.getGuild());

        if (event.getOption("force-update") != null && event.getOption("force-update").getAsBoolean() == true) {
            event.getHook().sendMessage("Updating ranks... May take a long time.").queue();
            rankService.updateAllRanks(server);
        }

        LOG.debug("Looking for ranks of all summoners in server {}", server.getName());        
        Set<Rank> ranks = rankService.getRanks(server);

        // check if there are any ranks
        if (ranks.isEmpty()) {
            event.reply("There are no ranks to show.").queue();
            return;
        }

        Set<MessageEmbed> embeds = build(ranks);

        if (embeds.isEmpty()) {
            event.reply("There are no ranks to show.").queue();
            return;
        }

        event.getHook().sendMessageEmbeds(embeds).queue();
    }

    private Set<MessageEmbed> build(Set<Rank> ranks) {
        // Divide the ranks into soloq and flex
        ArrayList<Rank> soloqRanks = new ArrayList<>();
        ArrayList<Rank> flexRanks = new ArrayList<>();
        for (Rank rank : ranks) {
            if (rank.getLeague() == null) {
                continue;
            }
            if (rank.getQueueType() == QueueType.RANKED_SOLO_5x5) {
                soloqRanks.add(rank);
            } else if (rank.getQueueType() == QueueType.RANKED_FLEX_SR) {
                flexRanks.add(rank);
            }
        }

        // Sort the ranks
        Collections.sort(soloqRanks, Collections.reverseOrder());
        Collections.sort(flexRanks, Collections.reverseOrder());

        // Build the embeds
        Set<MessageEmbed> embeds = new HashSet<>();
        if (!flexRanks.isEmpty()) {
            MessageEmbed flexEmbed = buildEmbed("👑 Flex Queue Leaderboard", flexRanks);
            embeds.add(flexEmbed);
        }
        if (!soloqRanks.isEmpty()) {
            MessageEmbed soloqEmbed = buildEmbed("👑 Solo Queue Leaderboard", soloqRanks);
            embeds.add(soloqEmbed);
        }

        return embeds;
    }

    @SuppressWarnings("null")
    // Suppressing null warning because the null check is done in the build method
    private MessageEmbed buildEmbed(String title, ArrayList<Rank> ranks) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(title);

        // First field is the rank
        StringBuilder sb = new StringBuilder();
        for (Rank rank : ranks) {
            sb.append(rank.getLeague().getTier().getEmoji())
                    .append(" ")
                    .append(rank.getLeague().getTier().getName())
                    .append(" ")
                    .append(rank.getLeague().getDivision())
                    .append(" (")
                    .append(rank.getLeaguePoints())
                    .append(" LP)\r\n");
        }
        builder.addField("", sb.toString(), true);

        sb = new StringBuilder();
        for (Rank rank : ranks) {
            // Build external link for summoner
            String normalizedRiotId = rank.getSummoner().getRiotId().replace("#", "-");
            String urlEncodedUsername = URLEncoder.encode(normalizedRiotId, StandardCharsets.UTF_8);
            String summonerLookupUrl = String.format(config.getSummonerLookupUrl(), urlEncodedUsername);
            sb.append("[")
                .append(rank.getSummoner().getRiotId())
                .append("](")
                .append(summonerLookupUrl)
                .append(") ")
                    .append("\n");
        }
        builder.addField("", sb.toString(), true);

        return builder.build();
    }

}
