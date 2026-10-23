package com.alistats.discorki.discord;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.result.GameStartNotificationResult;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;
import com.alistats.discorki.riot.MatchUtil;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.TemplatingService;
import com.alistats.discorki.util.ColorUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.basic.constants.types.lol.TeamType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

@Component
public class EmbedFactory {
    @Autowired
    TemplatingService templatingService;
    @Autowired
    ImageService imageService;
    final Logger LOG = LoggerFactory.getLogger(EmbedFactory.class);
    @Autowired
    private CustomConfigProperties config;
    @Autowired
    private GameConstantsController gameConstantsController;

    final Map<LaneType, String> roleEmojis = new EnumMap<>(Map.of(
            LaneType.TOP, "🛡️",
            LaneType.JUNGLE, "🌳",
            LaneType.MID, "🔥",
            LaneType.BOT, "🏹",
            LaneType.UTILITY, "❤️‍🩹"));

    public Set<MessageEmbed> getTeamPostGameNotificationEmbeds(TeamPostGameNotificationResult result) {
        EmbedBuilder builder = new EmbedBuilder();
        Set<MessageEmbed> embeds = new HashSet<>();

        // loop over hashmap
        for (Summoner summoner : result.getSubjects().keySet()) {
            MatchParticipant participant = result.getSubjects().get(summoner);
            String templatePath = getTemplatePath(result.getNotification().getName());
            // build template
            HashMap<String, Object> templateArgs = new HashMap<>();
            templateArgs.put("participant", participant);
            templateArgs.put("match", result.getMatch());
            templateArgs.put("champion", gameConstantsController.getChampionName(participant.getChampionId()));
            templateArgs.put("extraArgs", result.getExtraArguments());
            try {
                String description = templatingService.renderTemplate(templatePath, templateArgs);
                builder.setDescription(description);
            } catch (Exception e) {
                LOG.error("Error rendering template: {}", e.getMessage());
                continue;
            }

            builder.setTitle(result.getTitle());
            builder.setThumbnail(imageService.getChampionTileUrl(participant.getChampionId()).toString());
            builder.setColor(ColorUtil.generateRandomColorFromString(participant.getPuuid()));

            embeds.add(builder.build());
        }

        return embeds;
    }

    public MessageEmbed getPersonalPostGameNotificationEmbed(PersonalPostGameNotificationResult result) {
        EmbedBuilder builder = new EmbedBuilder();
        String templatePath = getTemplatePath(result.getNotification().getName());
        // build template
        HashMap<String, Object> templateArgs = new HashMap<>();
        templateArgs.put("summoner", result.getSubject());
        templateArgs.put("match", result.getMatch());
        templateArgs.put("extraArgs", result.getExtraArguments());
        try {
            String description = templatingService.renderTemplate(templatePath, templateArgs);
            builder.setDescription(description);
        } catch (Exception e) {
            LOG.error("Error rendering template: {}", e.getMessage());
        }
        if (result.getImage() != null) {
            builder.setImage(result.getImage().toString());
        }
        if (result.getThumbnail() != null) {
            builder.setThumbnail(result.getThumbnail().toString());
        }
        builder.setTitle(result.getTitle());

        return builder.build();
    }

    public Set<MessageEmbed> getGameStartNotificationEmbeds(GameStartNotificationResult result) {
        EmbedBuilder builder = new EmbedBuilder();
        Set<MessageEmbed> embeds = new HashSet<>();

        // for each subject in the result, create an embed
        for (Summoner summoner : result.getSubjects().keySet()) {
            String templatePath = getTemplatePath(result.getNotification().getName());
            // build template
            HashMap<String, Object> templateArgs = new HashMap<>();
            templateArgs.put("summoner", summoner);
            templateArgs.put("participant", result.getSubjects().get(summoner));
            templateArgs.put("match", result.getMatch());
            templateArgs.put("extraArgs", result.getExtraArguments());
            try {
                String description = templatingService.renderTemplate(templatePath, templateArgs);
                builder.setDescription(description);
            } catch (Exception e) {
                LOG.error("Error rendering template: {}", e.getMessage());
                continue;
            }

            builder.setTitle(result.getTitle());

            embeds.add(builder.build());
        }

        return embeds;
    }

    public MessageEmbed getMatchEmbed(Server server, LOLMatch match, Map<String, Rank> participantRanks) {
        EmbedBuilder builder = new EmbedBuilder();
        List<List<MatchParticipant>> teams = List.of(
                MatchUtil.getTeam(match, TeamType.BLUE),
                MatchUtil.getTeam(match, TeamType.RED));

        // Build fields
        builder.addField(buildTeamCompositionField(teams, server));
        builder.addField(buildDamageField(teams));
        builder.addField(buildRankField(teams, participantRanks));

        // Build thumbnail with map icon
        String mapThumbnail = imageService.getMapUrl(match.getMap().getId()).toString();
        builder.setThumbnail(mapThumbnail);

        // Build the footer
        builder.setFooter("Discorki - A FreshCoders endeavour");

        // Build the title
        boolean blueTeamWon = match.getTeams().stream()
                .anyMatch(team -> team.getTeamId() == TeamType.BLUE && team.didWin());
        String title = blueTeamWon ? "Blue team won!" : "Red team won!";
        Color color = blueTeamWon ? Color.BLUE : Color.RED;
        builder.setColor(color);
        builder.setTitle(title);

        // Build the description
        long durationInMinutes = MatchUtil.getDurationInMinutes(match);
        String matchUrl = String.format(config.getMatchLookupUrl(), match.getGameId());
        String description = "Match duration: " + durationInMinutes + " minutes.\n [Detailed game stats ↗️]("
                + matchUrl + ")\n*When no solo queue rank is found, flex queue rank will be shown.*";
        builder.setDescription(description);

        return builder.build();
    }

    private MessageEmbed.Field buildTeamCompositionField(List<List<MatchParticipant>> teams, Server server) {
        // Build blue side team composition
        StringBuilder fieldValue = new StringBuilder();
        buildTeamComposition(fieldValue, teams.get(0), server);
        fieldValue.append("\n\n**Red side**\n");
        buildTeamComposition(fieldValue, teams.get(1), server);

        // Assemble the field

        return new MessageEmbed.Field("Blue side", fieldValue.toString(), true);
    }

    private void buildTeamComposition(StringBuilder fieldValue, List<MatchParticipant> team, Server server) {
        for (MatchParticipant participant : team) {
            Player matchingPlayer = server.getPlayers().stream()
                    .filter(player -> player.getSummoners().stream()
                            .anyMatch(summoner -> summoner.getPuuid().equals(participant.getPuuid())))
                    .findFirst().orElse(null);

            fieldValue.append(buildSummonerFieldLine(participant, matchingPlayer));
        }
    }

    private String buildSummonerFieldLine(MatchParticipant participant, Player player) {
        // Build external link for summoner
        String summonerLookupUrl = config.formatSummonerLookupUrl(participant.getRiotIdName(),
                participant.getRiotIdTagline());

        StringBuilder str = new StringBuilder();
        String roleEmoji = roleEmojis.get(participant.getChampionSelectLane());
        if (roleEmoji != null) {
            str.append(roleEmoji);
        }

        // Mention tracked players, link everyone else by their Riot ID
        if (player != null) {
            str.append(" <@")
                    .append(player.getDiscordId())
                    .append("> [↗️](")
                    .append(summonerLookupUrl)
                    .append(") ");
        } else {
            str.append(" [")
                    .append(participant.getRiotIdName())
                    .append("](")
                    .append(summonerLookupUrl)
                    .append(") ");
        }

        str.append(gameConstantsController.getChampionName(participant.getChampionId()))
                .append("\n");

        return str.toString();
    }

    private MessageEmbed.Field buildDamageField(List<List<MatchParticipant>> teams) {
        StringBuilder fieldValue = new StringBuilder();

        // Build blue side damage
        for (MatchParticipant participant : teams.get(0)) {
            fieldValue
                    .append(NumberFormat.getIntegerInstance().format(participant.getTotalDamageDealtToChampions()))
                    .append("\n");
        }

        // Add spacing between the two teams
        fieldValue.append("\n\n\n");

        // Build red side damage
        for (MatchParticipant participant : teams.get(1)) {
            fieldValue
                    .append(NumberFormat.getIntegerInstance().format(participant.getTotalDamageDealtToChampions()))
                    .append("\n");
        }

        // Assemble the field
        return new MessageEmbed.Field("Damage", fieldValue.toString(), true);
    }

    private MessageEmbed.Field buildRankField(List<List<MatchParticipant>> teams,
            Map<String, Rank> participantRanks) {
        StringBuilder sb = new StringBuilder();

        // Build ranks for both teams
        for (List<MatchParticipant> team : teams) {
            for (MatchParticipant participant : team) {
                Rank rank = participantRanks.get(participant.getPuuid());
                if (rank != null) {
                    buildRankFieldLine(sb, rank);
                } else {
                    sb.append("-\n");
                }
            }
            sb.append("\n\n\n");
        }

        return new MessageEmbed.Field("Ranks", sb.toString(), true);
    }

    private void buildRankFieldLine(StringBuilder sb, Rank rank) {
        sb.append(rank.getLeague().getTier().getEmoji())
                .append("** ")
                .append(rank.getLeague().getTier().getName())
                .append(" ");
        if (!rank.getLeague().getTier().isApex()) {
            sb.append(rank.getLeague().getDivision());
        } else {
            sb.append("(").append(rank.getLeaguePoints()).append("LP)");
        }
        sb.append("  **");
        if (rank.getQueueType().equals(QueueType.RANKED_FLEX_SR)) {
            sb.append(" *(f)*");
        }
        sb.append("\n");
    }

    private String getTemplatePath(String notificationName) {
        return "templates/notifications/" + notificationName + ".pebble";
    }
}
