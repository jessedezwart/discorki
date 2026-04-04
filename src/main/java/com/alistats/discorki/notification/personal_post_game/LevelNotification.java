package com.alistats.discorki.notification.personal_post_game;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.PersonalPostGameNotificationResult;
import com.alistats.discorki.riot.dto.MatchDto;

@Component
public class LevelNotification extends Notification implements PersonalPostGameNotification {
    @Override
    public String getName() {
        return "LevelNotification";
    }
    @Override
    public String getFancyName() {
        return "Level notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner reaches a \"special\" level.";
    }

    @Override
    public Optional<PersonalPostGameNotificationResult> check(MatchDto match, Summoner summoner) {
        // we have the league api controller, and we want to use it to findbyPuuid
        long oldLevel = summoner.getLevel();

        try {
            long newLevel = leagueApiController.getSummonerByPuuid(summoner.getPuuid()).getSummonerLevel();
            if (!checkLevelCondition(oldLevel, newLevel)) return Optional.empty();

            summoner.setLevel(newLevel);
            summonerRepo.save(summoner);
            
            PersonalPostGameNotificationResult result = new PersonalPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubject(summoner);
            result.setTitle(String.format("%s has reached level %s!", summoner.getRiotId(), newLevel));
            
            return Optional.of(result);
        } catch (Exception e) {
            LOG.error("Error while checking level notification: {}", e.getMessage());
        }

        return Optional.empty();
    }
    
    private boolean checkLevelCondition(long oldLevel, long newLevel) {
        if (oldLevel == newLevel) {
            return false;
        }

        boolean centenaryMilestone = (newLevel % 100) == 0;
        boolean devilMilestone = newLevel == 666;
        boolean blazeItMilestone = newLevel == 420;
        boolean funnyNumberMilestone = newLevel == 69;

        return centenaryMilestone || devilMilestone || blazeItMilestone || funnyNumberMilestone;
    }
}
