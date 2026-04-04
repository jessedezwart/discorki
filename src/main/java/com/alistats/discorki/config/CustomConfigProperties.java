package com.alistats.discorki.config;

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
    public static final String SUMMONER_LOOKUP_URL = "https://op.gg/lol/summoners/euw/%s";
    @URL(message = "The url used by post game notifications for match lookup.")
    private String matchLookupUrl = "https://tracker.gg/lol/match/%s";
    @NotBlank
    @URL(message = "The domain or ip of the server hosting the application.")
    private String host;
    @URL(message = "The url used by post game notifications for match lookup.")
    private String[] developerDiscordIds;
}
