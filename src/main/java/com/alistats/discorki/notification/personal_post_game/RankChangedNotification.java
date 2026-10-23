package com.alistats.discorki.notification.personal_post_game;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;

import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;

@Component
public class RankChangedNotification extends Notification implements PersonalPostGameNotification {
    @Override
    public String getName() {
        return "RankChangedNotification";
    }
    @Override
    public String getFancyName() {
        return "Rank changed notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner's rank changes.";
    }
    
    @Autowired
    private ImageService imageService;
    @Autowired
    private RankService rankService;

    @Override
    public Optional<PersonalPostGameNotificationResult> check(LOLMatch match, Summoner summoner) {
        // Check if it was a ranked game
        Optional<QueueType> rankedQueueTypeOpt = QueueType.from(match.getQueue());
        if (rankedQueueTypeOpt.isEmpty()) {
            return Optional.empty();
        }
        QueueType rankedQueueType = rankedQueueTypeOpt.get();

        // Get latest rank from db
        Optional<Rank> currentRankOptional = rankService.getCurrentRank(summoner, rankedQueueType);

        // Find rank for queue type
        Optional<Rank> newRankOpt = rankService.fetchRank(summoner, rankedQueueType);

        if (!newRankOpt.isPresent()) {
            return Optional.empty();
        }

        // If there is no rank in the db, save it
        if (!currentRankOptional.isPresent()) {
            rankService.saveRank(summoner, newRankOpt.get());
            return Optional.empty();
        }

        Rank newRank = newRankOpt.get();

        // Compare rank
        Rank currentRank = currentRankOptional.get();
        int compareResult = currentRank.getLeague().compareTo(newRank.getLeague());
        String queueDescription = rankedQueueType.getName();

        if (compareResult == 0) {
            // Rank didn't change
            return Optional.empty();
        }

        // Save new rank
        rankService.saveRank(summoner, newRank);
        
        PersonalPostGameNotificationResult result = new PersonalPostGameNotificationResult();
        result.setNotification(this);
        result.setThumbnail(imageService.getRankEmblemUrl(newRank.getLeague().getTier()));
        result.setSubject(summoner);
        result.setMatch(match);
        // Depending on the result of the comparison, set the title
        if (compareResult > 0) {
            result.setTitle(String.format("%s just demoted to %s!", summoner.getName(), newRank.getLeague().getName()));
        } else {
            result.setTitle(String.format("%s just promoted to %s!", summoner.getName(), newRank.getLeague().getName()));
        }
        result.addExtraArgument("promoted", compareResult < 0);
        result.addExtraArgument("queueDescription", queueDescription);
        result.addExtraArgument("newRank", newRank);

        return Optional.of(result);
    }
}
