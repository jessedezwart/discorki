package com.alistats.discorki.discord;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.result.GameStartNotificationResult;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.ParticipantDto;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.TemplatingService;
import com.alistats.discorki.util.ColorUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

@Component
public class EmbedFactory {
    private final TemplatingService templatingService;
    private final ImageService imageService;
    final Logger LOG = LoggerFactory.getLogger(EmbedFactory.class);
    private final CustomConfigProperties config;
    private final GameConstantsController gameConstantsController;

    final HashMap<String, String> roleEmojis = new HashMap<>() {
        {
            put("TOP", "🛡️");
            put("JUNGLE", "🌳");
            put("MIDDLE", "🔥");
            put("BOTTOM", "🏹");
            put("UTILITY", "❤️‍🩹");
        }
    };

    public EmbedFactory(TemplatingService templatingService, ImageService imageService,
            CustomConfigProperties config, GameConstantsController gameConstantsController) {
        this.templatingService = templatingService;
        this.imageService = imageService;
        this.config = config;
        this.gameConstantsController = gameConstantsController;
    }

    public Set<MessageEmbed> getTeamPostGameNotificationEmbeds(TeamPostGameNotificationResult result) {
        EmbedBuilder builder = new EmbedBuilder();
        Set<MessageEmbed> embeds = new HashSet<>();

        // loop over hashmap
        for (Summoner summoner : result.getSubjects().keySet()) {
            ParticipantDto participant = result.getSubjects().get(summoner);
            String templatePath = getTemplatePath(result.getNotification().getName());
            // build template
            HashMap<String, Object> templateArgs = new HashMap<>();
            templateArgs.put("participant", participant);
            templateArgs.put("match", result.getMatch());
            templateArgs.put("champion", gameConstantsController.getChampionNameByKey(participant.getChampionId()));
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
            builder.setColor(ColorUtil.generateRandomColorFromString(participant.getSummonerId()));

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

    public MessageEmbed getMatchEmbed(Server server, MatchDto match, HashMap<ParticipantDto, Rank> particpantRanks) {
        EmbedBuilder builder = new EmbedBuilder();
        List<List<ParticipantDto>> teams = match.getInfo().getTeamCategorizedParticipants();

        // Build fields
        builder.addField(buildTeamCompositionField(teams, server));
        builder.addField(buildDamageField(teams));
        builder.addField(buildRankField(teams, particpantRanks));

        // Build thumbnail with map icon
        String mapThumbnail = imageService.getMapUrl(match.getInfo().getMapId()).toString();
        builder.setThumbnail(mapThumbnail);

        // Build the footer
        builder.setFooter("Discorki - A FreshCoders endeavour");

        // Build the title
        boolean blueTeamWon = match.getInfo().getTeams()[0].isWin();
        String title = blueTeamWon ? "Blue team won!" : "Red team won!";
        Color color = blueTeamWon ? Color.BLUE : Color.RED;
        builder.setColor(color);
        builder.setTitle(title);

        // Build the description
        int durationInMinutes = Math.round(match.getInfo().getGameDuration() / 60);
        String matchUrl = String.format(config.getMatchLookupUrl(), match.getMetadata().getMatchId());
        String description = "Match duration: " + durationInMinutes + " minutes.\n [Detailed game stats ↗️]("
                + matchUrl + ")\n*When no solo queue rank is found, flex queue rank will be shown.*";
        builder.setDescription(description);

        return builder.build();
    }

    private MessageEmbed.Field buildTeamCompositionField(List<List<ParticipantDto>> teams, Server server) {
        // Build blue side team composition
        StringBuilder fieldValue = new StringBuilder();
        buildTeamComposition(fieldValue, teams.get(0), server);
        fieldValue.append("\n\n**Red side**\n");
        buildTeamComposition(fieldValue, teams.get(1), server);

        // Assemble the field

        return new MessageEmbed.Field("Blue side", fieldValue.toString(), true);
    }

    private void buildTeamComposition(StringBuilder fieldValue, List<ParticipantDto> team, Server server) {
        for (ParticipantDto participant : team) {
            Player matchingPlayer = server.getPlayers().stream()
                    .filter(player -> player.getSummoners().stream()
                            .anyMatch(summoner -> summoner.getPuuid().equals(participant.getPuuid())))
                    .findFirst().orElse(null);
    
            if (matchingPlayer != null) {
                fieldValue.append(buildMentionedSummonerFieldLine(participant, participant.getTeamPosition(),
                        matchingPlayer));
            } else {
                fieldValue.append(buildSummonerFieldLine(participant, participant.getTeamPosition()));
            }
        }
    }
    

    private String buildMentionedSummonerFieldLine(ParticipantDto participant, String teamPosition, Player player) {
        StringBuilder str = new StringBuilder();
        if (teamPosition != null && !teamPosition.equals("")) {
            str.append(roleEmojis.get(teamPosition));
        }

        // Get champion name
        String championName = gameConstantsController.getChampionNameByKey(participant.getChampionId());
        String displayName = resolveParticipantDisplayName(participant);
        String summonerLookupUrl = buildSummonerLookupUrl(displayName);

        str.append(" <@")
                .append(player.getDiscordId())
                .append(">");
        if (summonerLookupUrl != null) {
            str.append(" [↗️](")
                    .append(summonerLookupUrl)
                    .append(")");
        }
        str.append(" ")
                .append(championName)
                .append("\n");

        return str.toString();
    }

    private String buildSummonerFieldLine(ParticipantDto participant, String teamPosition) {
        String displayName = resolveParticipantDisplayName(participant);
        String summonerLookupUrl = buildSummonerLookupUrl(displayName);
        // Get champion name
        String championName = gameConstantsController.getChampionNameByKey(participant.getChampionId());

        StringBuilder str = new StringBuilder();
        if (teamPosition != null && !teamPosition.equals("")) {
            str.append(roleEmojis.get(teamPosition));
        }

        str.append(" ");
        if (summonerLookupUrl != null) {
            str.append("[")
                .append(displayName)
                .append("](")
                .append(summonerLookupUrl)
                .append(") ");
        } else {
            str.append(displayName)
                .append(" ");
        }
        str
                .append(championName)
                .append("\n");

        return str.toString();
    }

    private String resolveParticipantDisplayName(ParticipantDto participant) {
        if (StringUtils.hasText(participant.getRiotId())) {
            return participant.getRiotId();
        }
        return participant.getSummonerName();
    }

    private String buildSummonerLookupUrl(String displayName) {
        String normalizedDisplayName = displayName.replace("#", "-");
        String urlEncodedUsername = URLEncoder.encode(normalizedDisplayName, StandardCharsets.UTF_8);
        return String.format(CustomConfigProperties.SUMMONER_LOOKUP_URL, urlEncodedUsername);
    }

    private MessageEmbed.Field buildDamageField(List<List<ParticipantDto>> teams) {
        StringBuilder fieldValue = new StringBuilder();

        // Build blue side damage
        for (ParticipantDto participant : teams.get(0)) {
            fieldValue
                    .append(NumberFormat.getIntegerInstance().format(participant.getTotalDamageDealtToChampions()))
                    .append("\n");
        }

        // Add spacing between the two teams
        fieldValue.append("\n\n\n");

        // Build red side damage
        for (ParticipantDto participant : teams.get(1)) {
            fieldValue
                    .append(NumberFormat.getIntegerInstance().format(participant.getTotalDamageDealtToChampions()))
                    .append("\n");
        }

        // Assemble the field
        return new MessageEmbed.Field("Damage", fieldValue.toString(), true);
    }

    private MessageEmbed.Field buildRankField(List<List<ParticipantDto>> teams,
            HashMap<ParticipantDto, Rank> summonerRanks) {
        StringBuilder sb = new StringBuilder();

        // Build ranks for both teams
        for (int i = 0; i < teams.size(); i++) {
            for (ParticipantDto participant : teams.get(i)) {
                if (summonerRanks.get(participant) != null) {
                    buildRankFieldLine(sb, summonerRanks.get(participant));
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
