package com.alistats.discorki.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.EnumUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Server;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.repository.RankRepo;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.dto.LeagueEntryDto;

@Service
public class RankService {
    @Autowired
    private LeagueApiController leagueApiController;
    @Autowired
    private RankRepo rankRepo;

    /**
     * Gets all summoners from a Discord server and updates their ranks.
     * Even if the rank didn't change, it will be updated in the database.
     * @param server The server to update the ranks for.
     */
    public void updateAllRanks(Server server) {
        Hibernate.initialize(server.getPlayers());

        Set<Player> players = server.getPlayers();

        for (Player player : players) {
            Set<Summoner> summoners = player.getSummoners();
            for (Summoner summoner : summoners) {
                updateRanks(summoner);
            }
        }
    }

    /**
     * Updates the ranks for a summoner
     * @param summoner The summoner to update ranks for
     */
    public void updateRanks(Summoner summoner) {
        Set<Rank> ranks = fetchRanks(summoner);
        for (Rank rank : ranks) {
            saveRank(summoner, rank);
        }
    }

    public Optional<Rank> getCurrentRank(Summoner summoner, QueueType queueType) {
        Optional<Rank> rank = rankRepo.findFirstBySummonerAndQueueTypeOrderByIdDesc(summoner, queueType);
        return rank;
    }

    /**
     * Saves a rank to the database. The rank is not updated with the latest data from the Riot API.
     * @param summoner The summoner to save the rank for.
     * @param rank The rank to save.
     */
    public void saveRank(Summoner summoner, Rank rank) {
        rank.setSummoner(summoner);
        rankRepo.save(rank);
    }

    /**
     * Takes a summoner and retrieves all ranks for that summoner from the Riot API.
     * The ranks are not saved to the database.
     * @param summoner The summoner to fetch ranks for.
     * @return A set of ranks for the summoner.
     */
    public Set<Rank> fetchRanks(Summoner summoner) {
        List<LeagueEntryDto> leagueEntries = Arrays
                .asList(leagueApiController.getLeagueEntriesByPuuid(summoner.getPuuid()));
        

        Set<Rank> ranks = new HashSet<>();

        for (LeagueEntryDto leagueEntry : leagueEntries) {
            // Skip non league ranks
            if (!EnumUtils.isValidEnum(QueueType.class, leagueEntry.getQueueType())) {
                continue;
            }
            Rank newRank = leagueEntry.toRank();
            newRank.setSummoner(summoner);
            ranks.add(newRank);
        }

        return ranks;
    }

    /**
     * Fetches a rank for a summoner and a queue type.
     * The rank is not saved to the database.
     * @param summoner The summoner to fetch the rank for.
     * @param queueType The queue type to fetch the rank for.
     * @return An optional rank.
     */
    public Optional<Rank> fetchRank(Summoner summoner, QueueType queueType) {
        Set<Rank> ranks = fetchRanks(summoner);
        for (Rank rank : ranks) {
            if (rank.getQueueType().equals(queueType)) {
                return Optional.of(rank);
            }
        }
        return Optional.empty();
    }

    
    /**
     * Gets all ranks for a server. The ranks are not updated with the latest data from the Riot API.
     * @param server The server to get ranks for.
     * @return A set of ranks for the server.
     */
    @Transactional(readOnly = true)
    public Set<Rank> getRanks(Server server) {
        Hibernate.initialize(server.getPlayers());

        Set<Rank> ranks = new HashSet<>();
        Set<Player> players = server.getPlayers();

        for (Player player : players) {
            Set<Summoner> summoners = player.getSummoners();
            for (Summoner summoner : summoners) {
                Rank soloqRank = summoner.getCurrentRank(QueueType.RANKED_SOLO_5x5);
                Rank flexqRank = summoner.getCurrentRank(QueueType.RANKED_FLEX_SR);
                if (soloqRank != null) {
                    ranks.add(soloqRank);
                }
                if (flexqRank != null) {
                    ranks.add(flexqRank);
                }
            }
        }

        return ranks;
    }
}
