package com.dreamgames.backendengineeringcasestudy.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dreamgames.backendengineeringcasestudy.dto.LeaderboardDTO;
import com.dreamgames.backendengineeringcasestudy.dto.UserResponseDTO;
import com.dreamgames.backendengineeringcasestudy.exception.UnauthorizedException;
import com.dreamgames.backendengineeringcasestudy.scheduler.TournamentScheduler;
import com.dreamgames.backendengineeringcasestudy.service.SecurityService;
import com.dreamgames.backendengineeringcasestudy.service.SecurityService.AuthJSON;
import com.dreamgames.backendengineeringcasestudy.service.TournamentService;
import com.dreamgames.backendengineeringcasestudy.service.UserService;
import com.dreamgames.backendengineeringcasestudy.util.DateUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
        Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);

    private final UserService userService;
    private final SecurityService securityService;
    private final TournamentService tournamentService;

    // CreateUserRequest
    @PostMapping("/v1/user")
    public ResponseEntity<UserResponseDTO> createNewUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null) {
            HttpInputMessage httpInputMessage = null;
            throw new HttpMessageNotReadableException("", httpInputMessage);
        }
        
        UserResponseDTO responseDTO = userService.createNewUser(username);
        logger.info("New user: %s".formatted(responseDTO));

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }
    
    // UpdateLevelRequest
    @PutMapping("/v1/user/{userID}")
    public ResponseEntity<UserResponseDTO> updateUserLevel(@PathVariable String userID, @RequestHeader(value="Authorization") String bearer) {   
        if (securityService.isAuthorized(userID, bearer)) {
            AuthJSON authJSON = securityService.decode(bearer);
            String country = authJSON.getCountry();

            UserResponseDTO responseDTO = userService.updateUserLevel(userID, country);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);    
        } else {
            throw new UnauthorizedException(); 
        }
    } 

    // GetGroupRankRequest
    @GetMapping("/v1/user/{userID}/tournament/rank")
    public ResponseEntity<Map<String, Integer>> getMethodName(@PathVariable String userID, @RequestParam String date, @RequestHeader(value="Authorization") String bearer) {
        if (securityService.isAuthorized(userID, bearer)) {
            LocalDateTime localDate = DateUtil.parseDateArg(date);
            
            int rank = tournamentService.getUserRank(UUID.fromString(userID), localDate);
            
            Map<String, Integer> response = new HashMap<>();
            response.put("rank", rank);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            throw new UnauthorizedException(); 
        }
    }

    // ClaimRewardRequest
    @PostMapping("/v1/user/claim-reward")
    public ResponseEntity<UserResponseDTO> claimReward(@RequestHeader(value="Authorization") String bearer) {
        AuthJSON authJSON = securityService.decode(bearer);
        UUID userID = authJSON.getUserID();

        tournamentService.claimReward(userID);
        UserResponseDTO progress = userService.getUserProgress(userID);

        return new ResponseEntity<>(progress, HttpStatus.OK);    
    }

    // GetGroupLeaderboardRequest
    @GetMapping("/v1/user/{userID}/tournament/group-leaderboard")
    public ResponseEntity<LeaderboardDTO> getMethodName(@PathVariable String userID, @RequestHeader(value="Authorization") String bearer) {
        if (securityService.isAuthorized(userID, bearer)) {
            LeaderboardDTO groupLeaderboard = tournamentService.getGroupLeaderboard(UUID.fromString(userID));
            return new ResponseEntity<>(groupLeaderboard, HttpStatus.OK);    
        } else {
            throw new UnauthorizedException(); 
        }
    }

}
