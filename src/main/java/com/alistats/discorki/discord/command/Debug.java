package com.alistats.discorki.discord.command;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.discord.command.shared.AbstractCommand;
import com.alistats.discorki.discord.command.shared.Command;
import com.alistats.discorki.model.Match;
import com.alistats.discorki.model.Match.Status;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.PlayerRepo;
import com.alistats.discorki.repository.RankRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;
import com.alistats.discorki.service.TemplatingService;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class Debug extends AbstractCommand implements Command{
    private final RankRepo rankRepo;

    public Debug(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService, RankRepo rankRepo) {
        super(leagueApiController, playerRepo, summonerRepo, serverRepo, matchRepo, gameConstantsController, config,
                templatingService, imageService, rankService);
        this.rankRepo = rankRepo;
    }

    @Override
    public String getCommandName() {
        return "debug";
    }

    @Transactional(readOnly = true)
    public void run(SlashCommandInteractionEvent event) {
        // Verify it's one of the allowed users
        if (!Arrays.asList(config.getDeveloperDiscordIds()).contains(event.getUser().getId())) {
            event.getHook().sendMessage("You are not allowed to use this command.").queue();
            return;
        }

        StringBuilder sb = new StringBuilder();

        // Get all database counts
        sb.append("**Database counts:**\n");
        sb.append("Guilds: ").append(serverRepo.count()).append("\n");
        sb.append("Users: ").append(playerRepo.count()).append("\n");
        sb.append("Summoners: ").append(summonerRepo.count()).append("\n");
        sb.append("Ranks: ").append(rankRepo.count()).append("\n");
        sb.append("Matches: ").append(matchRepo.count()).append("\n\n");        

        // Get games in progress
        sb.append("**Games in progress:**\n");
        Optional<Set<Match>> matchesInProgressOpt = matchRepo.findByStatus(Status.IN_PROGRESS);
        if (matchesInProgressOpt.isPresent()) {
            Set<Match> matchesInProgress = matchesInProgressOpt.get();
            for (Match match : matchesInProgress) {
                sb.append(match.getTrackedSummoners().size());
                sb.append(" summoner(s) - ");
                sb.append(match.getId());
                sb.append(" (");
                sb.append(match.getGameQueueConfigId());
                sb.append(")\n");
            }
        } else {
            sb.append("*None*\n");
        }

        // Send dm to user
        privateReply(event.getUser(), sb.toString());
        reply(event, "Debug information sent to your DMs.");
    }
}
