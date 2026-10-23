package com.alistats.discorki.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alistats.discorki.model.Player;
import com.alistats.discorki.model.Server;

public interface PlayerRepo extends JpaRepository<Player, Long> {
    Optional<Player> findByDiscordId(String discordId);
    Optional<Player> findByDiscordIdAndServer(String discordId, Server server);
}
