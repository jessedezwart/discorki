package com.alistats.discorki.riot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import no.stelar7.api.r4j.basic.utils.Utils;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

/**
 * Loads a match-v5 "info" payload the same way R4J does when it calls the API.
 */
public final class MatchFixtures {
    private MatchFixtures() {
    }

    public static LOLMatch match() {
        return match(json -> {
        });
    }

    public static LOLMatch match(Consumer<JsonObject> modifier) {
        try (InputStream in = MatchFixtures.class.getResourceAsStream("/match-v5-info.json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            modifier.accept(json);
            return Utils.getGson().fromJson(json, LOLMatch.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MatchParticipant participant(LOLMatch match, String puuid) {
        return match.getParticipants().stream()
                .filter(participant -> participant.getPuuid().equals(puuid))
                .findFirst()
                .orElseThrow();
    }
}
