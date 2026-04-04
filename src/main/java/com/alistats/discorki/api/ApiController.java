package com.alistats.discorki.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alistats.discorki.api.dto.InfoDto;
import com.alistats.discorki.model.Match.Status;
import com.alistats.discorki.repository.MatchRepo;
import com.alistats.discorki.repository.ServerRepo;
import com.alistats.discorki.repository.SummonerRepo;

@RestController
@RequestMapping("/api")
@ResponseStatus(HttpStatus.OK)
public class ApiController {
    private final SummonerRepo summonerRepo;
    private final ServerRepo serverRepo;
    private final MatchRepo matchRepo;

    public ApiController(SummonerRepo summonerRepo, ServerRepo serverRepo, MatchRepo matchRepo) {
        this.summonerRepo = summonerRepo;
        this.serverRepo = serverRepo;
        this.matchRepo = matchRepo;
    }

    @GetMapping("/info")
    public InfoDto getAllEntries() {
        InfoDto info = new InfoDto();
        info.setMatches(matchRepo.count());
        info.setMatchesInProgress(matchRepo.countByStatus(Status.IN_PROGRESS));
        info.setServers(serverRepo.count());
        info.setSummoners(summonerRepo.count());
        return info;
    }
}
