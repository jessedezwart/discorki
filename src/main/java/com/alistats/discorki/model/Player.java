package com.alistats.discorki.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name= "players", uniqueConstraints = { @UniqueConstraint(columnNames = { "discordId", "server_id" }) })
public class Player {
    @Id
    @GeneratedValue
    private long id;
    private String discordId;
    private String discordUsername;
    @ManyToMany
    @JoinTable(
        name = "player_riot_account",
        joinColumns = @JoinColumn(name = "player_id"),
        inverseJoinColumns = @JoinColumn(name = "riot_account_puuid")
    )
    private Set<Summoner> summoners = new HashSet<>();
    @ManyToOne
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    public void addSummoner(Summoner summoner) {
        summoners.add(summoner);
    }

    public void removeSummoner(Summoner summoner) {
        summoners.remove(summoner);
    }

    public boolean hasSummoner(Summoner summoner) {
        return summoners.contains(summoner);
    }

    public Player(User user, Server server) {
        this.discordId = user.getId();
        this.discordUsername = user.getName();
        this.server = server;
    }
}
