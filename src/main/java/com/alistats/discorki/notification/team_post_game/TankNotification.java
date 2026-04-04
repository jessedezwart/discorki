package com.alistats.discorki.notification.team_post_game;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alistats.discorki.model.Summoner;
import com.alistats.discorki.notification.Notification;
import com.alistats.discorki.notification.result.TeamPostGameNotificationResult;
import com.alistats.discorki.repository.SummonerRepo;
import com.alistats.discorki.riot.controller.GameConstantsController;
import com.alistats.discorki.riot.controller.LeagueApiController;
import com.alistats.discorki.riot.dto.MatchDto;
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.ParticipantDto;
import com.alistats.discorki.service.ImageService;

@Component
public class TankNotification extends Notification implements TeamPostGameNotification {
    public TankNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
    }

    @Override
    public String getName() {
        return "TankNotification";
    }
    @Override
    public String getFancyName() {
        return "SuperSoaker notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a player takes the most damage in a game.";
    }

    public final int TRESHOLD_DMG_TAKEN_PER_MINUTE = 7000;

    @Override
    public Optional<TeamPostGameNotificationResult> check(MatchDto match, HashMap<Summoner, ParticipantDto> trackedParticipants) {
        List<ParticipantDto> participants = Arrays.asList(match.getInfo().getParticipants());
        ParticipantDto maxDamageTaken = Collections.max(participants,
                Comparator.comparing(
                        s -> (s.getTotalDamageTaken() +
                                s.getDamageSelfMitigated())));

        // Check if max damage taken is above the treshold
        long minutesPlayed = match.getInfo().getGameDuration() / 60;
        int totalDamageSoaked = maxDamageTaken.getTotalDamageTaken() + maxDamageTaken.getDamageSelfMitigated();
        if (totalDamageSoaked / minutesPlayed < TRESHOLD_DMG_TAKEN_PER_MINUTE) {
            return Optional.empty();
        }
        
        for (Summoner summoner : trackedParticipants.keySet()) {
            if (trackedParticipants.get(summoner).getSummonerName().equals(maxDamageTaken.getSummonerName())) {
                TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
                result.setNotification(this);
                result.setMatch(match);
                result.setTitle("SuperSoaker!");
                result.addSubject(summoner, maxDamageTaken);

                // Add damage treshold and total damage soaked to extra arguments
                result.addExtraArgument("treshold", TRESHOLD_DMG_TAKEN_PER_MINUTE);
                result.addExtraArgument("totalDamageSoaked", totalDamageSoaked);
                
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }
}
