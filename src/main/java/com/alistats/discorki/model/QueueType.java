package com.alistats.discorki.model;

import java.util.Optional;

import lombok.Getter;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

@Getter
public enum QueueType {
    RANKED_SOLO_5x5("Ranked Solo/Duo"),
    RANKED_FLEX_SR("Ranked Flex");

    private final String name;

    QueueType(String name) {
        this.name = name;
    }

    /**
     * Maps both the queue of a match and the queue of a league entry to a ranked queue type.
     */
    public static Optional<QueueType> from(GameQueueType gameQueueType) {
        if (gameQueueType == null) {
            return Optional.empty();
        }
        return switch (gameQueueType) {
            case RANKED_SOLO_5X5, TEAM_BUILDER_RANKED_SOLO -> Optional.of(RANKED_SOLO_5x5);
            case RANKED_FLEX_SR -> Optional.of(RANKED_FLEX_SR);
            default -> Optional.empty();
        };
    }
}
