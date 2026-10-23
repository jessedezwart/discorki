package com.alistats.discorki.riot;

import java.util.Optional;

/**
 * A Riot ID as typed by a user, e.g. "Faker#KR1".
 */
public record RiotId(String gameName, String tagLine) {

    public static Optional<RiotId> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        int separator = input.lastIndexOf('#');
        if (separator <= 0 || separator == input.length() - 1) {
            return Optional.empty();
        }
        String gameName = input.substring(0, separator).trim();
        String tagLine = input.substring(separator + 1).trim();
        if (gameName.isEmpty() || tagLine.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RiotId(gameName, tagLine));
    }

    @Override
    public String toString() {
        return gameName + "#" + tagLine;
    }
}
