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
public class TopDpsNotification extends Notification implements TeamPostGameNotification {
    public TopDpsNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
    }

    @Override
    public String getName() {
        return "TopDpsNotification";
    }
    @Override
    public String getFancyName() {
        return "Top DPS notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner gets the most damage in a game";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(MatchDto match, HashMap<Summoner, ParticipantDto> trackedParticipants) {
        List<ParticipantDto> participants = Arrays.asList(match.getInfo().getParticipants());
        ParticipantDto maxDamageDealt = Collections.max(participants,
                Comparator.comparing(
                        ParticipantDto::getTotalDamageDealtToChampions));
        
        for (Summoner summoner : trackedParticipants.keySet()) {
            if (trackedParticipants.get(summoner).getSummonerName().equals(maxDamageDealt.getSummonerName())) {
                TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
                result.setNotification(this);
                result.setMatch(match);
                result.setTitle("Top DPS!");
                result.addSubject(summoner, maxDamageDealt);
                
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }
}
