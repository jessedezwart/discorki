package com.alistats.discorki.model;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name= "matches")
public class Match {
    public enum Status {
        IN_PROGRESS, FINISHED
    }

    @Id
    private long id;
    @ManyToMany
    @JoinTable(
        name = "match_riot_account",
        joinColumns = @JoinColumn(name = "match_id"),
        inverseJoinColumns = @JoinColumn(name = "riot_account_puuid")
    )
    private Set<Summoner> trackedSummoners;
    private Status status;
    private long gameQueueConfigId;
}
