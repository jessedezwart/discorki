package com.alistats.discorki.riot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RiotIdTest {

    @Test
    void parsesGameNameAndTagLine() {
        RiotId riotId = RiotId.parse(" Hide on bush#KR1 ").orElseThrow();

        assertEquals("Hide on bush", riotId.gameName());
        assertEquals("KR1", riotId.tagLine());
        assertEquals("Hide on bush#KR1", riotId.toString());
    }

    @Test
    void usesLastHashAsSeparator() {
        RiotId riotId = RiotId.parse("we#are#EUW").orElseThrow();

        assertEquals("we#are", riotId.gameName());
        assertEquals("EUW", riotId.tagLine());
    }

    @Test
    void rejectsInputWithoutTagLine() {
        assertTrue(RiotId.parse("Faker").isEmpty());
        assertTrue(RiotId.parse("Faker#").isEmpty());
        assertTrue(RiotId.parse("#KR1").isEmpty());
        assertTrue(RiotId.parse(null).isEmpty());
    }
}
