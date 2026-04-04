package com.alistats.discorki.notification.personal_post_game;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.QueueType;
import com.alistats.discorki.model.Rank;
import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.service.ImageService;
import com.alistats.discorki.service.RankService;

@Component
public class RankChangedNotification extends Notification implements PersonalPostGameNotification {
    private final RankService rankService;

    public RankChangedNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController,
            RankService rankService) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
        this.rankService = rankService;
    }

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
    @Override
    public Optional<PersonalPostGameNotificationResult> check(MatchDto match, Summoner summoner) {
        // Check if it was a ranked game
        if (!match.getInfo().isRanked()) {
            return Optional.empty();
        }

        // Get queue type of match
        QueueType rankedQueueType = QueueType.getQueueType(match.getInfo().getQueueId());

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
        String queueDescription = leagueGameConstantsController.getQueue(match.getInfo().getQueueId())
                .getDescription();

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
            result.setTitle(String.format("%s just demoted to %s!", summoner.getRiotId(), newRank.getLeague().getName()));
        } else {
            result.setTitle(String.format("%s just promoted to %s!", summoner.getRiotId(), newRank.getLeague().getName()));
        }
        result.addExtraArgument("promoted", compareResult < 0);
        result.addExtraArgument("queueDescription", queueDescription);
        result.addExtraArgument("newRank", newRank);

        return Optional.of(result);
    }
}
