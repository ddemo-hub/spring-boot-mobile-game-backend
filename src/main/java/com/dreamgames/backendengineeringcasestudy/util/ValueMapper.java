package com.dreamgames.backendengineeringcasestudy.util;

import java.util.Optional;

import org.springframework.data.redis.core.DefaultTypedTuple;

import com.dreamgames.backendengineeringcasestudy.dto.LeaderboardDTO;
import com.dreamgames.backendengineeringcasestudy.dto.UserResponseDTO;
import com.dreamgames.backendengineeringcasestudy.entity.User;
import com.dreamgames.backendengineeringcasestudy.entity.UserInLeaderboard;
import com.dreamgames.backendengineeringcasestudy.enums.Country;

public class ValueMapper {

    /**
     * Construct a UserResponseDTO object with the given parameters
     * @param user The User entity
     * @param authToken The auth token for the user
     * @return UserResponseDTO
     */
    public static UserResponseDTO userToDTO (User user, String authToken) {
        String countryString = Optional.ofNullable(user.getCountry())
                                .map(Country::toString)
                                .orElse(null);

        UserResponseDTO dto = UserResponseDTO.builder()
                                .userID(user.getUser_id())
                                .username(user.getUsername())
                                .coins(user.getCoins())
                                .level(user.getLevel())
                                .country(countryString)
                                .authToken(authToken)
                                .build();

        return dto;
    }

    /**
     * Construct a LeaderboardDTO object with type "group" for the given leaderboard
     * @param leaderboard An Object array of UserInLeaderboard objects. 
     * @return LeaderboardDTO
     */
    public static LeaderboardDTO groupLeaderboardToDTO(Object[] leaderboard) {
        LeaderboardDTO leaderboardDTO = new LeaderboardDTO("group");

        for (int i = 0; i < 5; i++) {
            UserInLeaderboard user = (UserInLeaderboard) leaderboard[i];
            leaderboardDTO.addElementToLeaderboard(user.getUser_id(), user.getUsername(), user.getCountry(), user.getScore());
        }
        
        return leaderboardDTO;
    }

    /**
     * Construct a LeaderboardDTO object with type "country" for the given leaderboard
     * @param leaderboard An Object array of DefaultTypedTuple objects.
     * @return LeaderboardDTO
     */
    public static LeaderboardDTO countryLeaderboardToDTO(Object[] leaderboard) {
        LeaderboardDTO leaderboardDTO = new LeaderboardDTO("country");

        for (int i = 0; i < 5; i++) {
            @SuppressWarnings("rawtypes")
            DefaultTypedTuple country = (DefaultTypedTuple) leaderboard[i];
            
            leaderboardDTO.addElementToLeaderboard((String) country.getValue(), (int) Math.round(country.getScore()));
        }
        
        return leaderboardDTO;

    }
    
}
