package com.dreamgames.backendengineeringcasestudy.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dreamgames.backendengineeringcasestudy.entity.TournamentGroup;
import com.dreamgames.backendengineeringcasestudy.entity.User;
import com.dreamgames.backendengineeringcasestudy.entity.UserInLeaderboard;
import com.dreamgames.backendengineeringcasestudy.entity.UserInTournament;
import com.dreamgames.backendengineeringcasestudy.entity.UserInTournament.UserInTournamentID;
import com.dreamgames.backendengineeringcasestudy.repository.TournamentGroupRepository;
import com.dreamgames.backendengineeringcasestudy.repository.UserInTournamentRepository;
import com.dreamgames.backendengineeringcasestudy.repository.UserRepository;
import com.dreamgames.backendengineeringcasestudy.service.RedisService;

import lombok.RequiredArgsConstructor;

/**
 * A background task responsible of forming tournament groups  
 */
@Component
@RequiredArgsConstructor
public class GroupFormationScheduler {
    Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisService redisService;

    private final UserRepository userRepository;
    private final TournamentGroupRepository tournamentGroupRepository;
    private final UserInTournamentRepository userInTournamentRepository;

    /**
     * This function is called by the schedulerf if a group can be formed with the given users
     * @param userIDs List of userID strings of the group members
     */
    @Async
    public void updateStorage(String[] userIDs) {
        // Initialize operation templates for value and  operations 

        // Convert userID strings to UUID representations
        List<UUID> uuidList = new ArrayList<>();
        for (String userID : userIDs) {
            UUID uuid = UUID.fromString(userID);
            uuidList.add(uuid);
        }
        List<User> users = userRepository.findByUserIDs(uuidList);
        
        // Insert a new group to the tournament_group table and get the ID generated for it by the DBMS
        Long groupID;
        try {
            TournamentGroup tournamentGroup = tournamentGroupRepository.save(new TournamentGroup());
            groupID = tournamentGroup.getGroup_id();
        } catch (JpaSystemException ex) {
            // If the following illegal action is detected by the database trigger,
            // it means that there must exists severe vulnerabilities in the previous checks.  
            logger.error("(TournamentGroupRepository) Attempted illegal group formation outside permitted hours (00:00 to 20:00 UTC) detected by database trigger!");
            throw new IllegalStateException("Daily tournaments are held between 00.00 and 20.00 UTC");
        }

        // Create a new leaderboard Hash in Redis with groupID as the key
        // For every user, create a UserInLeaderboard object and put it into the hash 
        List<UserInTournament> uitList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User user = users.get(i);

            UserInTournamentID uitID = new UserInTournamentID(groupID, user.getUser_id());
            UserInTournament uit = new UserInTournament();
            uit.setId(uitID);
            uitList.add(uit);            
 
            UserInLeaderboard leaderboardUser = new UserInLeaderboard();
            leaderboardUser.setUser_id(user.getUser_id().toString());
            leaderboardUser.setUsername(user.getUsername());
            leaderboardUser.setCountry(user.getCountry().toString());
            redisService.insertUserInGroupLeaderboard(groupID.toString(), user.getUser_id().toString(), leaderboardUser);
        }

        // Once the group hash is finalized, cache the key value pairs of (userID, groupID) for every user
        // This operation must be performed after, or in the some transaction as the leaderboard creation
        // since the (userID, groupID) pairs are also used to determine if a user is assigned to a group or not
        // see waitForOtherPlayers(String userID, String country) in TournamentService
        for (User user: users) {
            redisService.setUserGroup(user.getUser_id().toString(), groupID);
        }

        try {
            // Once the cache is set, bulk insert/update the data to the persistant storage 
            userInTournamentRepository.saveAll(uitList);
            userRepository.collectEntranceFeeByUserIds(uuidList);
        } catch (JpaSystemException ex) {
            // If the following illegal action is detected by the database trigger,
            // it means that there must exists severe vulnerabilities in the previous checks.  
            logger.error("(UserInTournamentRepository) Attempted illegal group formation outside permitted hours (00:00 to 20:00 UTC) detected by database trigger!");
            throw new IllegalStateException("Daily tournaments are held between 00.00 and 20.00 UTC");
        }

        logger.info("A new group with group ID '%s' has been formed with the users: %s".formatted(groupID, uitList));
    }

    /**
     * Periodically check the country waiting queues to see if a group can be formed 
     */
    @Scheduled(fixedDelayString = "${scheduler.groupFormationFrequency}")
    public void startGroupFormationScheduler() {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
          
        // Since a modifying operation is going to be performed based on the output of a read operation,
        // a Redis transaction must be used to prevent Read after Write (RAW) hazard  
        Object executionResult = redisTemplate.execute((RedisConnection connection) -> {
            // Begin transaction
            connection.multi();

            if (zSetOps.zCard("Turkey") > 0 && zSetOps.zCard("Germany") > 0 && zSetOps.zCard("France") > 0 
                    && zSetOps.zCard("the United States") > 0 && zSetOps.zCard("the United Kingdom") > 0) {
                
                // Queue pop operations if every country waiting queue has at least one user waiting
                connection.zSetCommands().zPopMin("Turkey".getBytes());
                connection.zSetCommands().zPopMin("Germany".getBytes());
                connection.zSetCommands().zPopMin("France".getBytes());
                connection.zSetCommands().zPopMin("the United States".getBytes());
                connection.zSetCommands().zPopMin("the United Kingdom".getBytes());
            }

            // Execute all pop operations at once if the resources were not already poped by another thread in the mean time 
            return connection.exec();
        });
    
        if (executionResult != null) {
            // Every country's waiting queue has at least one user in the line, a group can be formed
            @SuppressWarnings("unchecked")
            ArrayList<DefaultTuple> usersArray = (ArrayList<DefaultTuple>) executionResult;

            String[] userIDs = new String[5];
            for (int i = 0; i < 5; i++){
                // Serialize the user ID value into a string
                userIDs[i] = new String(usersArray.get(i).getValue()).substring(1, 37);
            }

            updateStorage(userIDs);
        }
    }
}