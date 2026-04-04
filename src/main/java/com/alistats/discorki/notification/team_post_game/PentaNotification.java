package com.alistats.discorki.notification.team_post_game;

import java.util.HashMap;
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

/**
 * A summoner got a penta in the last game
 */
@Component
public class PentaNotification extends Notification implements TeamPostGameNotification {
    public PentaNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
    }

    @Override
    public String getName() {
        return "PentaNotification";
    }
    @Override
    public String getFancyName() {
        return "Penta notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when a summoner got a penta in the last game.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(MatchDto match, HashMap<Summoner, ParticipantDto> trackedParticipants) {
        HashMap<Summoner, ParticipantDto> subjects = new HashMap<>();
        for (Summoner summoner : trackedParticipants.keySet()) {
            ParticipantDto participant = trackedParticipants.get(summoner);
            if (participant.getPentaKills() > 0) {
                subjects.put(summoner, participant);
            }
        }

        // if there are any tracked participants left, notify
        if (trackedParticipants.size() > 0) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(subjects);
            result.setTitle("Penta!");

            return Optional.of(result);
        }

        return Optional.empty();
    }
}
