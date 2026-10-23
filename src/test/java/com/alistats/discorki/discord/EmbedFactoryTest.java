package com.alistats.discorki.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alistats.discorki.config.CustomConfigProperties;
import com.alistats.discorki.model.Division;
import com.alistats.discorki.model.League;
import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.model.Tier;
import com.alistats.discorki.riot.MatchFixtures;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.service.ImageService;

import net.dv8tion.jda.api.entities.MessageEmbed;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;

class EmbedFactoryTest {

    @Test
    void buildsMatchEmbed() throws Exception {
        GameConstantsController gameConstants = mock(GameConstantsController.class);
        when(gameConstants.getChampionName(anyInt())).thenReturn("Champ");
        ImageService imageService = mock(ImageService.class);
        when(imageService.getMapUrl(anyInt())).thenReturn(URI.create("https://example.com/map11.png").toURL());

        EmbedFactory embedFactory = new EmbedFactory();
        ReflectionTestUtils.setField(embedFactory, "gameConstantsController", gameConstants);
        ReflectionTestUtils.setField(embedFactory, "imageService", imageService);
        ReflectionTestUtils.setField(embedFactory, "config", new CustomConfigProperties());

        // One tracked player in this server
        Summoner summoner = new Summoner();
        summoner.setPuuid("puuid-adc-blue");
        Player player = new Player();
        player.setDiscordId("1234");
        player.setSummoners(Set.of(summoner));
        Server server = new Server();
        server.setPlayers(Set.of(player));

        Rank rank = new Rank();
        rank.setQueueType(QueueType.RANKED_FLEX_SR);
        rank.setLeague(new League(Division.IV, Tier.EMERALD));

        LOLMatch match = MatchFixtures.match();
        MessageEmbed embed = embedFactory.getMatchEmbed(server, match, Map.of("puuid-mid-red", rank));

        assertEquals("Blue team won!", embed.getTitle());
        String teams = embed.getFields().get(0).getValue();
        assertTrue(teams.contains("🏹 <@1234> [↗️](https://op.gg/lol/summoners/euw/Blue%20Adc-EUW) Champ"), teams);
        assertTrue(teams.contains("🔥 [Red Mid](https://op.gg/lol/summoners/euw/Red%20Mid-EUW) Champ"), teams);
        String ranks = embed.getFields().get(2).getValue();
        assertTrue(ranks.contains("Emerald IV"), ranks);
        assertTrue(embed.getDescription().contains("30 minutes"), embed.getDescription());
    }
}
