package com.dreamgames.backendengineeringcasestudy.dto;

import java.util.ArrayList;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@JsonInclude(Include.NON_NULL)
public class LeaderboardDTO {
    
    @Data
    @AllArgsConstructor
    private class UserInGroup {
        private final String userID;
        private final String username;
        private final String country;        
        private final int score;
    }

    @Data
    @AllArgsConstructor
    private class CountryWithScore {
        private final String country;
        private final int score;
    }

    private ArrayList<UserInGroup> groupLeaderboard;
    private ArrayList<CountryWithScore> countryLeaderboard;

    @JsonIgnore
    private String leaderboardType;

    public LeaderboardDTO(String leaderboardType) {
        switch (leaderboardType) {
            case "group" -> {
                this.leaderboardType = leaderboardType;
                this.groupLeaderboard = new ArrayList<>();
            }
            case "country" -> {
                this.leaderboardType = leaderboardType;
                this.countryLeaderboard = new ArrayList<>();
            }
            default -> throw new RuntimeException("The 'leaderboardType' variable must be set to either 'group' for a group leaderboard or 'country' for a country leaderboard");
        }
    }

    public boolean addElementToLeaderboard(String userID, String username, String country, int score) {
        if (!leaderboardType.equals("group")) {
            throw new RuntimeException("Cannot add a user element to a leaderboard of type 'country'");
        }

        if (groupLeaderboard.size() == 5) {
            return false;
        }
        UserInGroup newUser = new UserInGroup(userID, username, country, score);
        
        try {
            this.groupLeaderboard.add(newUser);
            if (groupLeaderboard.size() == 5) {
                sortLeaderboard();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addElementToLeaderboard(String country, int score) {
        if (!leaderboardType.equals("country")) {
            throw new RuntimeException("Cannot add a country element to a leaderboard of type 'group'");
        }

        if (countryLeaderboard.size() == 5) {
            return false;
        }
        CountryWithScore newCountry = new CountryWithScore(country, score);
        
        try {
            this.countryLeaderboard.add(newCountry);
            if (countryLeaderboard.size() == 5) {
                sortLeaderboard();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void sortLeaderboard() {
        if (leaderboardType.equals("group")) {
            groupLeaderboard.sort(Comparator.comparingInt(UserInGroup::getScore).reversed());
        } else if (leaderboardType.equals("country")) {
            countryLeaderboard.sort(Comparator.comparingInt(CountryWithScore::getScore).reversed());
        }
    }


}
