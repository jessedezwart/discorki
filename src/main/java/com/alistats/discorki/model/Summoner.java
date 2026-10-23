package com.alistats.discorki.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.alistats.discorki.model.Match.Status;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Riot account that is tracked by Discorki. Identified by its PUUID, since
 * summoner names, summoner ids and account ids have been retired by Riot.
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "riot_accounts")
public class Summoner {
    @Id
    private String puuid;
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long revisionDate;
    private long level;
    @OneToMany(mappedBy = "summoner")
    private Set<Rank> ranks = new HashSet<>();
    @ManyToMany(mappedBy = "trackedSummoners")
    private Set<Match> matches = new HashSet<>();
    @ManyToMany(mappedBy = "summoners")
    private Set<Player> players = new HashSet<>();

    /**
     * @return the Riot ID, e.g. "Faker#KR1"
     */
    public String getName() {
        if (tagLine == null || tagLine.isBlank()) {
            return gameName;
        }
        return gameName + "#" + tagLine;
    }

    public boolean hasRiotId(String gameName, String tagLine) {
        return this.gameName != null && this.gameName.equalsIgnoreCase(gameName)
                && this.tagLine != null && this.tagLine.equalsIgnoreCase(tagLine);
    }

    public Optional<Match> getMatchInProgress() {
        return matches.stream().filter(m -> m.getStatus() == Status.IN_PROGRESS).findFirst();
    }

    public Rank getCurrentRank(QueueType queueType) {
        return ranks.stream()
            .filter(r -> {
                QueueType type = r.getQueueType();
                return type != null && type.equals(queueType);
            })
            .max((a, b) -> Long.compare(a.getId(), b.getId()))
            .orElse(null);
    }

    public void removeLinkedPlayer(Player player) {
        players.remove(player);
    }

    public void removeLinkedPlayerById(Long playerId) {
        players.removeIf(player -> player.getId() == playerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Summoner other)) {
            return false;
        }
        return puuid != null && puuid.equals(other.puuid);
    }

    @Override
    public int hashCode() {
        return puuid == null ? 0 : puuid.hashCode();
    }
}
