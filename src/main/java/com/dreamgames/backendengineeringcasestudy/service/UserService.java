package com.dreamgames.backendengineeringcasestudy.service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dreamgames.backendengineeringcasestudy.dto.UserResponseDTO;
import com.dreamgames.backendengineeringcasestudy.entity.User;
import com.dreamgames.backendengineeringcasestudy.entity.UserInLeaderboard;
import com.dreamgames.backendengineeringcasestudy.enums.Country;
import com.dreamgames.backendengineeringcasestudy.exception.DatabaseExpection;
import com.dreamgames.backendengineeringcasestudy.repository.UserRepository;
import com.dreamgames.backendengineeringcasestudy.util.ValueMapper;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;  

    private final RedisService redisService;
    private final SecurityService securityService;;

    private final Country[] countries = Country.values();
    private Country getRandomCountry() {
        int randomIndex = new Random().nextInt(countries.length);
        return countries[randomIndex];
    }

    /**
     * Create a new user with the given username
     * @param username User name of the new user 
     * @return DTO for the newly created user
     */
    public UserResponseDTO createNewUser(String username) {
        Country randomCountry = getRandomCountry();

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setCountry(randomCountry);

        User newUserInDB;
        try {
            newUserInDB = userRepository.save(newUser);
        } catch (JpaSystemException | DataIntegrityViolationException ex ) {
            throw new DatabaseExpection("Invalid username. The username must be at least 1 character long, " + 
                                            "must not exceed 32 characters and must only contain ASCII characters.");
        }

        // Create the authentication token
        String authToken = securityService.encode(newUserInDB.getUser_id(), newUserInDB.getUsername(), randomCountry.toString());

        return ValueMapper.userToDTO(newUserInDB, authToken);
    }

    /**
     * Increment the score of the user's country in Country Leaderboard sorted and increment the user's score in his/her group's leaderboard hash 
     * @param userID User ID of the user
     * @param country Country of the user
     * @param groupID Group ID of the user
     */
    @Async
    private void updateTournamentScores(String userID, String country, String groupID) {
        // Update cache
        redisService.updateCountryLeaderboard(country, Double.valueOf(1));

        UserInLeaderboard uil = redisService.getUserInGroupLeaderboard(groupID, userID);
        if (uil != null) {
            uil.increment();
            redisService.insertUserInGroupLeaderboard(groupID, userID, uil);
        } else {

        }
    }

    /**
     * Update the user's level by 1; if the user is in a tournament, update the persistant storage and the cache accordingly
     * @param userID ID of the user
     * @param country Country of the user
     * @return A DTO for the progress (the new level and the new score)
     */
    public UserResponseDTO updateUserLevel(String userID, String country) {
        UserResponseDTO progress;  

        // Update persistent storage
        int affectedRows = userRepository.updateUserLevel(UUID.fromString(userID));
        if (affectedRows == 1) {
            Long groupID = redisService.getUserGroup(userID, true);
            if (groupID != null) {
                // Update the cache
                updateTournamentScores(userID, country, groupID.toString());
            }

            progress = getUserProgress(UUID.fromString(userID));
        } else { 
            throw new DatabaseExpection("User with the user ID %s could not be found in the database".formatted(userID));
        }

        return progress;
    }

    public User getUser(UUID userID) {
        return userRepository.findById(userID)
                                .orElseThrow(() -> new DatabaseExpection("User not found"));
    }

    /**
     * Return the level and coins values of the user with the given user ID
     * @param userID ID of the user
     * @return A DTO for the progress (user's level and score)
     */
    public UserResponseDTO getUserProgress(UUID userID) {
        Map<String, Object> result = userRepository.findLevelAndCoinsByUserId(userID);

        User user = new User();
        int newLevel = (int) result.get("level");
        long newCoins = (long) result.get("coins");
        user.setLevel(newLevel);
        user.setCoins(newCoins);

        return ValueMapper.userToDTO(user, null);
    }

}
