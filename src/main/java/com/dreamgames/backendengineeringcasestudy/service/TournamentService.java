package com.dreamgames.backendengineeringcasestudy.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dreamgames.backendengineeringcasestudy.dto.LeaderboardDTO;
import com.dreamgames.backendengineeringcasestudy.entity.User;
import com.dreamgames.backendengineeringcasestudy.exception.IllegalActionException;
import com.dreamgames.backendengineeringcasestudy.exception.RequestTimeoutException;
import com.dreamgames.backendengineeringcasestudy.repository.TournamentGroupRepository;
import com.dreamgames.backendengineeringcasestudy.repository.UserInTournamentRepository;
import com.dreamgames.backendengineeringcasestudy.scheduler.TournamentScheduler;
import com.dreamgames.backendengineeringcasestudy.util.DateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentService {
    Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);

    @Value("${scheduler.groupFormationFrequency}")
    private String groupFormationFreq;

    @Value("${scheduler.waitForOtherPlayersTimeout}")
    private String waitForOtherPlayersTimeout;

    private final RedisService redisService;

    private final TournamentGroupRepository tournamentGroupRepository;
    private final UserInTournamentRepository userInTournamentRepository;

    /**
     * Loop until the user is assigned to a group or the waitForOtherPlayersTimeout timeouts 
     * @param userID - user_id field of the user
     * @param country - country assigned to the user
     * @return group id
     */
    @Async
    private CompletableFuture<Long> waitForOtherPlayers(String userID, String country) {
        // Looping frequency is (group formation frequency) / 2, a reference to the Nyquist Theorem
        int loopFrequency = Integer.parseInt(groupFormationFreq) / 2;

        // Number of loopFrequency in waitForOtherPlayersTimeout, to convert time into number of trials
        int maxTrial = Integer.parseInt(waitForOtherPlayersTimeout) / loopFrequency;

        // Loop until a group is formed
        logger.info("User with id %s from %s joins the queue in order to enter the tournament".formatted(userID, country));

        int trial = 0;
        while (true) {     
            if (trial >= maxTrial) {
                redisService.removeFromCountryQueue(country, userID);
                logger.warn("The tournament entry request of user %s from %s has been timed out".formatted(userID, country));
                throw new RequestTimeoutException("A timeout occured while waiting for other players");
            } else { trial++; }
            
            Long groupID = redisService.getUserGroup(userID, false);
            if (groupID != null) {
                logger.info("User %s from %s has entered the tournament. Waiting time: %sms".formatted(userID, country, loopFrequency*trial));
                return CompletableFuture.completedFuture(groupID);
            }
            try {
                Thread.sleep(loopFrequency);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Check if the user is allowed to participate in a tournament
     * @param user The user entity
     * @throws IllegalActionException
     */
    private void isAllowedInTournament(User user) throws IllegalActionException {
        LocalDateTime currentTimeUtc = DateUtil.getCurrentTimeUTC();
        if (currentTimeUtc.getHour() >= 20) {
            throw new IllegalActionException("Daily tournaments are held between 00.00 and 20.00 UTC");
        }

        if (user.getLevel() < 20){
            throw new IllegalActionException("You must be at least level 20 in order to participate in a tournament");
        } 
        else if (user.getCoins() < 1000) {
            throw new IllegalActionException("It costs 1000 coins to participate in a tournament");
        }
        
        Long groupID = redisService.getUserGroup(user.getUser_id().toString(), true);
        if (groupID != null) {
            throw new IllegalActionException("You are already in the tournament");
        }

        if (userInTournamentRepository.existsUnclaimedRewards(user.getUser_id()) == true) {
            throw new IllegalActionException("You have unclaimed rewards. You must claim your rewards from the previous tournament to participate in the new one");
        }
    }

    /**
     * Place the user into the queue, wait for other players, read and return the group leaderboard created by the GroupFormationScheduler
     * @param user The user entity
     * @return Group leaderboard in DTO format
     */
    public LeaderboardDTO enterTournament(User user) {
        UUID userID = user.getUser_id();

        // Check if the user is eligible to enter a tournament
        try {
            isAllowedInTournament(user);
        } catch (IllegalActionException ex) {
            throw ex;
        }

        // Add the user to his/her country queue 
        boolean is_added = redisService.insertToCountryQueue(user.getCountry().toString(), userID.toString());

        if (!is_added) {
            throw new IllegalActionException("You are already in a queue");
        }

        CompletableFuture<Long> groupIDPromise = waitForOtherPlayers(userID.toString(), user.getCountry().toString());
        CompletableFuture.allOf(groupIDPromise).join();

        // Convert the group leaderboard to a DTO
        LeaderboardDTO leaderboardDTO = new LeaderboardDTO("group");
        try {
            String groupID = groupIDPromise.get().toString();

            // Get the group leaderboard
            return redisService.getGroupLeaderboard(groupID, false);
        } catch (InterruptedException | ExecutionException e) {} 

        return leaderboardDTO;        
    }

    /**
     * Get the group_id of the group the user with userID is assigned to 
     * @param userID The user entity
     * @return Group leaderboard in DTO format
     */
    public LeaderboardDTO getGroupLeaderboard(UUID userID) {
        Long groupID = redisService.getUserGroup(userID.toString(), true);

        if (groupID == null) {
            throw new IllegalActionException("You are not in the tournament at the moment");
        }

        // Get the group leaderboard
        return redisService.getGroupLeaderboard(groupID.toString(), false);
    }

    /** Get the country leaderboard */
    public LeaderboardDTO getCountryLeaderboard() {
        // Check if a tournament is active
        LocalDateTime currentTimeUtc = DateUtil.getCurrentTimeUTC();
        if (currentTimeUtc.getHour() >= 20) {
            throw new IllegalActionException("No active tournaments... Daily tournaments are held between 00.00 and 20.00 UTC");
        }

        return redisService.getCountryLeaderboard();
    }

    /**
     * Get the rank of the user with the id userID for a tournament held in the given date
     * @param userID The user entity
     * @param date The date that the tournament was held
     * @return user's rank
     */
    public int getUserRank(UUID userID, LocalDateTime date) {
        Optional<Long> groupID = tournamentGroupRepository.getTournamentGroupIDByDate(userID, date);

        if (groupID.isEmpty()) {
            throw new IllegalActionException("You were not in a tournament on %s".formatted(date));
        }
        return userInTournamentRepository.findUserRankInGroup(userID, date);
    }

    /**
     * Update the persistant storage to claim the reward (update the user's coins and set is_reward_claimed to true)
     * @param userID The user entity
     */
    public void claimReward(UUID userID) {
        boolean existsUnclaimedRewards = userInTournamentRepository.existsUnclaimedRewards(userID);
        if (!existsUnclaimedRewards) {
            throw new IllegalActionException("No rewards to claim");
        }

        userInTournamentRepository.claimReward(userID);
    }

}
