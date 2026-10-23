package com.alistats.discorki.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Component
@Getter
@Setter
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "app")
public class CustomConfigProperties {
    // Formatted with the game name and the tag line of a Riot ID
    private String summonerLookupUrl = "https://op.gg/lol/summoners/euw/%s-%s";
    @URL(message = "The url used by post game notifications for match lookup.")
    private String matchLookupUrl = "https://tracker.gg/lol/match/EUW/%d";
    @NotBlank
    @URL(message = "The domain or ip of the server hosting the application.")
    private String host;
    private String[] developerDiscordIds = new String[0];

    public String formatSummonerLookupUrl(String gameName, String tagLine) {
        return String.format(summonerLookupUrl, encodePathSegment(gameName), encodePathSegment(tagLine));
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
