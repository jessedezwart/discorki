package com.alistats.discorki.discord.command;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.EmbedFactory;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.riot.RiotId;
import com.alistats.discorki.riot.controller.LeagueApiHelper;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.shared.RiotAccount;

@Component
public class Latest extends AbstractCommand implements Command {
    @Autowired
    private EmbedFactory embedFactory;

    @Autowired
    private LeagueApiHelper apiHelper;

    @Override
    public String getCommandName() {
        return "latest";
    }

    @Transactional(readOnly = true)
    public void run(SlashCommandInteractionEvent event) throws Exception {
        String riotIdInput = Optional.ofNullable(event.getOption("riot-id"))
                .orElseThrow(() -> new RuntimeException("Riot ID cannot be empty.")).getAsString();
        Optional<RiotId> riotId = RiotId.parse(riotIdInput);
        if (riotId.isEmpty()) {
            reply(event, String.format("***%s*** is not a valid Riot ID. Use the format `name#tag`.", riotIdInput));
            return;
        }

        LOG.debug("Looking for account {}", riotId.get());
        RiotAccount account = leagueApiController.getAccountByRiotId(riotId.get().gameName(), riotId.get().tagLine());
        if (account == null) {
            reply(event, String.format("Riot ID ***%s*** not found.", riotId.get()));
            return;
        }

        LOG.debug("Getting most recent match for {} ({}).", riotId.get(), account.getPUUID());
        LOLMatch match = apiHelper.getMostRecentMatch(account.getPUUID());
        if (match == null) {
            reply(event, String.format("No matches found for ***%s***.", riotId.get()));
            return;
        }

        Server server = obtainServer(event.getGuild());
        event.getHook().sendMessage("Fetching player ranks... 💤").queue();
        LOG.debug("Fetching player ranks for match {}", match.getGameId());
        Map<String, Rank> participantRanks = apiHelper.getParticipantRanks(match.getParticipants());

        LOG.debug("Creating embed for match {}", match.getGameId());
        MessageEmbed embed = embedFactory.getMatchEmbed(server, match, participantRanks);

        LOG.debug("Sending embed for match {}", match.getGameId());
        event.getHook().sendMessageEmbeds(embed).queue();
    }

}
