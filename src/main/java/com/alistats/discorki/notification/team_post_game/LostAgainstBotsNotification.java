package com.alistats.discorki.notification.team_post_game;

import java.util.Arrays;
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
import com.alistats.discorki.riot.dto.MatchDto.InfoDto.TeamDto;
import com.alistats.discorki.service.ImageService;

// Check if summoner lost custom or coop vs AI
@Component
public class LostAgainstBotsNotification extends Notification implements TeamPostGameNotification {
    public LostAgainstBotsNotification(LeagueApiController leagueApiController, SummonerRepo summonerRepo,
            ImageService imageService, GameConstantsController leagueGameConstantsController) {
        super(leagueApiController, summonerRepo, imageService, leagueGameConstantsController);
    }

    @Override
    public String getName() {
        return "LostAgainstBotsNotification";
    }

    @Override
    public String getFancyName() {
        return "Lost against bots notification";
    }

    @Override
    public String getDescription() {
        return "Notifies when a summoner lost against bots.";
    }

    @Override
    public Optional<TeamPostGameNotificationResult> check(MatchDto match,
            HashMap<Summoner, ParticipantDto> trackedParticipants) {
        if (didAFullBotTeamWin(match)) {
            TeamPostGameNotificationResult result = new TeamPostGameNotificationResult();
            result.setNotification(this);
            result.setMatch(match);
            result.setSubjects(trackedParticipants);
            result.setTitle("Lost against bots");

            return Optional.of(result);
        }

        return Optional.empty();
    }

    private boolean didAFullBotTeamWin(MatchDto match) {
        List<TeamDto> teams = Arrays.asList(match.getInfo().getTeams());
        return teams.stream()
                .filter(TeamDto::isWin)
                .anyMatch(
                        team -> {
                            List<ParticipantDto> participants = Arrays.asList(match.getInfo().getParticipants());

                            // Assuming here that an empty team qualifies as a "full bot team"
                            return participants.stream()
                                    .filter(p -> p.getTeamId() == team.getTeamId())
                                    .allMatch(p -> p.getPuuid().equals("BOT"));
                        }

                );
    }
}
