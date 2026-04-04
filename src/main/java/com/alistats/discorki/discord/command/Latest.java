package com.alistats.discorki.discord.command;

import java.util.HashMap;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.discord.EmbedFactory;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.riot.controller.LeagueApiHelper;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.ParticipantDto;
import com.alistats.discorki.riot.dto.SummonerDto;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
        String riotIdInput = Optional.ofNullable(event.getOption("summoner"))
                .orElseThrow(() -> new RuntimeException("Summoner cannot be empty.")).getAsString();

        String[] parts = riotIdInput.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            reply(event, "Invalid Riot ID format. Use `GameName#TAG` (e.g. `PlayerName#EUW`).");
            return;
        }
        String gameName = parts[0].trim();
        String tagLine = parts[1].trim();

        try {
            LOG.debug("Looking for summoner {}#{}", gameName, tagLine);
            var accountDto = leagueApiController.getAccountByRiotId(gameName, tagLine);
            SummonerDto summonerDto = leagueApiController.getSummonerByPuuid(accountDto.getPuuid());
            LOG.debug("Getting most recent match for summoner {}#{} ({})", gameName, tagLine, summonerDto.getPuuid());
            MatchDto match = apiHelper.getMostRecentMatch(summonerDto.getPuuid());
            Server server = obtainServer(event.getGuild());
            event.getHook().sendMessage("Fetching player ranks... 💤").queue();
            LOG.debug("Fetching player ranks for match {}", match.getInfo().getGameId());
            HashMap<ParticipantDto, Rank> participantRanks = apiHelper.getParticipantRanks(match.getInfo().getParticipants());

            LOG.debug("Creating embed for match {}", match.getInfo().getGameId());
            MessageEmbed embed = embedFactory.getMatchEmbed(server, match, participantRanks);
            if (embed == null) {
                throw new Exception("Could not create embed for match.");
            }

            LOG.debug("Sending embed for match {}", match.getInfo().getGameId());
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                reply(event, String.format("Summoner ***%s#%s*** not found.", gameName, tagLine));
            } else {
                throw new Exception(e);
            }
        }
    }

}
