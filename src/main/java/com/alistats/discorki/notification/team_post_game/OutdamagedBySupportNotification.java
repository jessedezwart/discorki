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

@Component
public class OutdamagedBySupportNotification extends Notification implements TeamPostGameNotification {
    public OutdamagedBySupportNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
    }

    @Override
    public String getName() {
        return "OutdamagedBySupportNotification";
    }
    @Override
    public String getFancyName() {
        return "Outdamaged by support notification";
    }
    @Override
    public String getDescription() {
        return "Notifies when you are outdamaged by your support.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(MatchDto match, HashMap<Summoner, ParticipantDto> trackedParticipants) {
        // Since we COULD potentially have tracked players on opposite teams:
        // one being support on red, one being adc on blue, we SHOULD check
        // on team-basis. But this is extremely unlikely. For now, we can
        // just check on role and assume the tracked participants are on the same team.
        // After, we check team-equality, so we only have false negatives, no false
        // positives
        ParticipantDto bottom = null;
        ParticipantDto support = null;

        // Loop through tracked participants
        for (ParticipantDto participant : trackedParticipants.values()) {
            // Check if participant is adc
            if (participant.getTeamPosition().equals("BOTTOM")) {
                bottom = participant;
            }
            // Check if participant is support
            else if (participant.getTeamPosition().equals("UTILITY")) {
                support = participant;
            }
        }

        // Support and bottom are not tracked, or they belong to different teams
        if (support == null || bottom == null || support.getTeamId() != bottom.getTeamId()) {
            return Optional.empty();
        }

        // Check if support did more damage than adc
        if (support.getTotalDamageDealtToChampions() > bottom.getTotalDamageDealtToChampions()) {
            for (Summoner summoner : trackedParticipants.keySet()) {
                if (trackedParticipants.get(summoner).getSummonerName().equals(support.getSummonerName())) {
                    TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
                    result.setNotification(this);
                    result.setMatch(match);
                    result.addSubject(summoner, support);
                    result.setTitle("Outdamaged by support!");
                    result.addExtraArgument("bottom", bottom);
                    result.addExtraArgument("support", support);

                    return Optional.of(result);
                }
            }
        }
        
        return Optional.empty();
    }
}
