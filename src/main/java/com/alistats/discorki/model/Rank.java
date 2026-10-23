package com.alistats.discorki.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "riot_account_ranks")
public class Rank implements Comparable<Rank> {
    @Id
    @GeneratedValue
    private long id;
    @ManyToOne
    @JoinColumn(name = "riot_account_puuid", nullable = false)
    private Summoner summoner;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(32)")
    private QueueType queueType;
    @Embedded
    private League league;
    private int leaguePoints;
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public int getTotalLp() {
        int value = 0;

        value += league.getDivision().getDivisionLpValue();
        value += league.getTier().getTierLpValue();
        value += getLeaguePoints();

        return value;
    }

    @Override
    public int compareTo(Rank rank) {
        return this.getTotalLp() - rank.getTotalLp();
    }

}
