package com.dreamgames.backendengineeringcasestudy.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dreamgames.backendengineeringcasestudy.entity.UserInLeaderboard;
import com.dreamgames.backendengineeringcasestudy.repository.UserInTournamentRepository;
import com.dreamgames.backendengineeringcasestudy.service.RedisService;
import com.dreamgames.backendengineeringcasestudy.util.DateUtil;

import lombok.RequiredArgsConstructor;

/**
 * Synchronizes the cache with the persistant storage for a tournament
 */
@Component
@RequiredArgsConstructor
public class TournamentScheduler {
    Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);

    private final RedisService redisService;
    private final UserInTournamentRepository userInTournamentRepository;


    /** Give the first and second place users their rewards and clear the cache for the new day's tournament at 00.00 */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC") 
    public void updateRewards() {
        // Get the current DateTime in UTC
        LocalDateTime currentTimeUtc = DateUtil.getCurrentTimeUTC();

        List<UUID> rankOneUserIDs = userInTournamentRepository.findRankOneUserIDs(currentTimeUtc);
        userInTournamentRepository.updateRewards(rankOneUserIDs, 10000);

        List<UUID> rankTwoUserIDs = userInTournamentRepository.findRankTwoUserIDs(currentTimeUtc);
        userInTournamentRepository.updateRewards(rankTwoUserIDs, 5000);

        logger.info("First-place users of the last tournament: %s".formatted(rankOneUserIDs));
        logger.info("Second-place users of the last tournament: %s".formatted(rankTwoUserIDs));

        logger.info("New tournament begins");
        redisService.flushTournamentCache();
    }

    /** 
     * Initialize the cache for the current day's tournament with the data stored in the persistant storage; 
     * so that in the case of a system failure, the cache can be utilized directly after re-start
    */
    @EventListener(ApplicationReadyEvent.class)
    public void loadTournamentCache() {
        // Clear the cache before the initialization
        redisService.flushTournamentCache();

        // Get the current DateTime in UTC
        LocalDateTime currentTimeUtc = DateUtil.getCurrentTimeUTC();

        // Select the users who are in a group in the current tournament
        List<Object[]> userDetails = userInTournamentRepository.findAllUserDetailsInTournament(currentTimeUtc);
        
        // Calculate the country scores for the current tournament
        List<Object[]> countryScores = userInTournamentRepository.findCountryTotalScoresInTournament(currentTimeUtc);
        
        // Initialize the cache for the Country Leaderboard
        if (countryScores.isEmpty()) {
            redisService.initCountryLeaderboard();
            logger.info("The 'Country Leaderboard' cache has been initialized with zeros");
        }
        else {
            for (Object[] countryObj: countryScores) {
                // countryObj[0] is the Country 
                redisService.updateCountryLeaderboard(countryObj[0].toString(), Double.valueOf(countryObj[1].toString()));
            }    
            logger.info("The 'Country Leaderboard' cache has been initialized");
        }

        // TBD: Instead of loading every user at start and waiting for inactive data to be deleted after the TTL,
        // a mechanism to load only the data of users who were active before the system failure can be implemented 
        for (Object[] userObj: userDetails) {
            // First, cache the key value pair of (userID, groupID) for every user 
            redisService.setUserGroup(userObj[1].toString(), Long.valueOf(userObj[0].toString()));

            // Then, cache the group leaderboard
            UserInLeaderboard leaderboardUser = new UserInLeaderboard();
            leaderboardUser.setUser_id(userObj[1].toString());
            leaderboardUser.setUsername((String) userObj[2]);
            leaderboardUser.setCountry(userObj[3].toString());
            leaderboardUser.setScore((Integer) userObj[4]);
            
            // userObj[0].toString() is the group ID and userObj[1].toString() is the user ID
            redisService.insertUserInGroupLeaderboard(userObj[0].toString(), userObj[1].toString(), leaderboardUser);
        }
        logger.info("The cache for the group leaderboards have been initialized");
    }
}
