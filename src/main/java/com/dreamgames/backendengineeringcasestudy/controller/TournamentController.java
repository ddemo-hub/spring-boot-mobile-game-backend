package com.dreamgames.backendengineeringcasestudy.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreamgames.backendengineeringcasestudy.dto.LeaderboardDTO;
import com.dreamgames.backendengineeringcasestudy.entity.User;
import com.dreamgames.backendengineeringcasestudy.scheduler.TournamentScheduler;
import com.dreamgames.backendengineeringcasestudy.service.SecurityService;
import com.dreamgames.backendengineeringcasestudy.service.SecurityService.AuthJSON;
import com.dreamgames.backendengineeringcasestudy.service.TournamentService;
import com.dreamgames.backendengineeringcasestudy.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TournamentController {
    Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);

    private final UserService userService;
    private final SecurityService securityService;
    private final TournamentService tournamentService;

    // EnterTournamentRequest
    @PostMapping("/v1/tournament/enter")
    public ResponseEntity<LeaderboardDTO> enterTournament(@RequestHeader(value="Authorization") String bearer) {
        AuthJSON authJSON = securityService.decode(bearer);
        UUID userID = authJSON.getUserID();

        User user = userService.getUser(userID);

        LeaderboardDTO leaderboardDTO = tournamentService.enterTournament(user);
        logger.info("User with id %s enters the tournament".formatted(userID));
        return new ResponseEntity<>(leaderboardDTO, HttpStatus.OK);
    }

    // GetCountryLeaderboardRequest
    @GetMapping("/v1/tournament/country-leaderboard")
    public ResponseEntity<LeaderboardDTO> getGroupLeaderboard(@RequestHeader(value="Authorization") String bearer) {
        securityService.decode(bearer);

        LeaderboardDTO countryLeaderboard = tournamentService.getCountryLeaderboard();
        return new ResponseEntity<>(countryLeaderboard, HttpStatus.OK);
    }
    
}
