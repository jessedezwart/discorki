package com.alistats.discorki.discord.command.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.discord.SlashCommandController;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.PlayerRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;
import com.alistats.discorki.service.TemplatingService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class AbstractCommand {
    protected final LeagueApiController leagueApiController;
    protected final PlayerRepo playerRepo;
    protected final SummonerRepo summonerRepo;
    protected final ServerRepo serverRepo;
    protected final MatchRepo matchRepo;
    protected final GameConstantsController gameConstantsController;
    protected final CustomConfigProperties config;
    protected final TemplatingService templatingService;
    protected final ImageService imageService;
    protected final RankService rankService;

    protected final Logger LOG = LoggerFactory.getLogger(SlashCommandController.class);

    protected AbstractCommand(LeagueApiController leagueApiController, PlayerRepo playerRepo, SummonerRepo summonerRepo,
            ServerRepo serverRepo, MatchRepo matchRepo, GameConstantsController gameConstantsController,
            CustomConfigProperties config, TemplatingService templatingService, ImageService imageService,
            RankService rankService) {
        this.leagueApiController = leagueApiController;
        this.playerRepo = playerRepo;
        this.summonerRepo = summonerRepo;
        this.serverRepo = serverRepo;
        this.matchRepo = matchRepo;
        this.gameConstantsController = gameConstantsController;
        this.config = config;
        this.templatingService = templatingService;
        this.imageService = imageService;
        this.rankService = rankService;
    }

    @SuppressWarnings("null")
    protected Server obtainServer(net.dv8tion.jda.api.entities.Guild guild) {
        LOG.debug("Obtaining server for guild {}", guild.getName());
        return serverRepo.findById(guild.getId()).orElseGet(() -> {
            if (guild.getDefaultChannel() == null) {
                LOG.error("Guild {} has no default channel, cannot create server", guild.getName());
                return null;
            }

            Server newServer = new Server();
            newServer.setId(guild.getId());
            newServer.setName(guild.getName());
            newServer.setDefaultChannelId(guild.getDefaultChannel().getIdLong());

            return serverRepo.save(newServer);
        });
    }

    /**
     * Reply to the user with a message. This function exists to prevent the null
     * type safety warnings
     *
     */
    @SuppressWarnings("null")
    // The null type safety warnings are suppressed because all commands are guild only and message can never be null
    protected void reply(SlashCommandInteractionEvent event, String message) {
        LOG.info("Sending response for {} to {} in server {}", event.getCommandString(), event.getInteraction().getChannel(), event.getGuild().getName());
        event.getHook().sendMessage(message).queue();
    }

    @SuppressWarnings("null")
    // Message can never be null
    protected void privateReply(User recipient, String message) {
        LOG.info("Sending private response to {}", recipient.getName());
        recipient.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue());
    }
}
