package com.dreamgames.backendengineeringcasestudy.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dreamgames.backendengineeringcasestudy.dto.LeaderboardDTO;
import com.dreamgames.backendengineeringcasestudy.entity.UserInLeaderboard;
import com.dreamgames.backendengineeringcasestudy.repository.UserInTournamentRepository;
import com.dreamgames.backendengineeringcasestudy.scheduler.TournamentScheduler;
import com.dreamgames.backendengineeringcasestudy.util.DateUtil;
import com.dreamgames.backendengineeringcasestudy.util.ValueMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Interactions with Redis are performed through this service to make sure that the TTL is reset at every operation, so that active users
 * are always being served from the memory. Plus, the data related on the user and the tournament are very closely related and it is a good idea to
 * cache them together. For further scalability, this service is a better solution than a @Cacheable annotation based one     
 */
@Service
@RequiredArgsConstructor
public class RedisService {
    Logger logger = LoggerFactory.getLogger(TournamentScheduler.class);
    
    @Value("${ttl.userGroup}")
    private String userGroupTTL;

    @Value("${ttl.groupLeaderboard}")
    private String groupLeaderboardTTL;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserInTournamentRepository userInTournamentRepository;

    private HashOperations<String, String, Object> hashOps;
    private ValueOperations<String, Object> valueOps;
    private ZSetOperations<String, Object> zSetOps;

    @PostConstruct
    public void initializeRedisOperations() {
        this.zSetOps = redisTemplate.opsForZSet();
        this.hashOps = redisTemplate.opsForHash();
        this.valueOps = redisTemplate.opsForValue();
    }
    
    /** Clear the cache */
    public void flushTournamentCache() {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory != null) {
            connectionFactory.getConnection().serverCommands().flushAll();
        } 
        logger.info("Redis flushed");
    }

    /** Get the country leaderboard from the memory.
     * The country leaderboard always lives in the memory since its size is static
     * @return LeaderboardDTO of the country leaderboard
     */
    public LeaderboardDTO getCountryLeaderboard() {
        Set<TypedTuple<Object>> countryLeaderboardSet = zSetOps.reverseRangeByScoreWithScores("Country Leaderboard", 0, Integer.MAX_VALUE);

        if (countryLeaderboardSet != null) {
            Object[] countryLeaderboard = countryLeaderboardSet.toArray();
            return ValueMapper.countryLeaderboardToDTO(countryLeaderboard);    
        } else {
            logger.error("The country leaderboard could not be initialized on start");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error occurred. Country leaderboard cannot be accessed");
        }    
    }

    /** Initialize the country leaderboard with zeros */
    public void initCountryLeaderboard() {
        zSetOps.add("Country Leaderboard", "Turkey", 0);
        zSetOps.add("Country Leaderboard", "Germany", 0);
        zSetOps.add("Country Leaderboard", "France", 0);
        zSetOps.add("Country Leaderboard", "the United States", 0);
        zSetOps.add("Country Leaderboard", "the United Kingdom", 0);
    }

    public Double updateCountryLeaderboard(String country, Double value) {
        return zSetOps.incrementScore("Country Leaderboard", country, value);
    }

    /**
     * Read the group leaderboard from the cache; if not in cache, read from from the persistent storage and update the cache
     * @param groupID Group ID
     * @param checkPersistentStorage If true, chech the  persistent storage for the leaderboard if it is not in the cache or not
     * @return LeaderboardDTO object of the group's leaderboard OR null if no group is found with the ID in the active tournament
     */
    public LeaderboardDTO getGroupLeaderboard(String groupID, boolean checkPersistentStorage) {
        Map<String, Object> hashValues = hashOps.entries(groupID);
        if (hashValues.isEmpty()) {
            if (checkPersistentStorage) {
                boolean loadSuccess = loadGroupToRedis(Long.valueOf(groupID));
                if(!loadSuccess) {
                    return null;
                }
                hashValues = hashOps.entries(groupID);    
            } else {
                return  null;
            }
        }

        // Set the TTL
        redisTemplate.expire(groupID, Integer.parseInt(groupLeaderboardTTL), TimeUnit.SECONDS);

        return ValueMapper.groupLeaderboardToDTO(hashValues.values().toArray());
    }

    /**
     * Read the user with userID from the active tournament group with group ID. Check both the cache and persistent storage.
     * @param groupID
     * @param userID
     * @return UserInLeaderboard object of the user or null
     */
    public UserInLeaderboard getUserInGroupLeaderboard(String groupID, String userID) {
        UserInLeaderboard uil = (UserInLeaderboard) hashOps.get(groupID, userID);
        if (uil == null) {
            boolean loadSuccess = loadUserToRedis(userID);
            if(!loadSuccess) {
                return null;
            }
            uil = (UserInLeaderboard) hashOps.get(groupID, userID);
        }        
        redisTemplate.expire(groupID, Integer.parseInt(groupLeaderboardTTL), TimeUnit.SECONDS);

        return uil;
    }

    public void insertUserInGroupLeaderboard(String groupID, String userID, UserInLeaderboard leaderboardUser) {
        hashOps.put(groupID, userID, leaderboardUser);
        redisTemplate.expire(groupID, Integer.parseInt(groupLeaderboardTTL), TimeUnit.SECONDS);
    }

    public void setUserGroup(String userID, Long groupID) {
        valueOps.set(userID, groupID, Integer.parseInt(userGroupTTL), TimeUnit.SECONDS);
    }
    
    /**
     * Get the ID the group that the user is assigned to in the active tournament
     * @param userID
     * @param checkPersistentStorage
     * @return ID of the group that the user is assigned to, null if not in a group
     */
    public Long getUserGroup(String userID, boolean checkPersistentStorage) {
        Object groupID = valueOps.get(userID);
        if (groupID != null) {
            Long longGroupID = Long.valueOf(groupID.toString());

            setUserGroup(userID, longGroupID);
            return longGroupID;
        } else {
            if (checkPersistentStorage) {
                boolean loadSuccess = loadUserToRedis(userID);
                if (loadSuccess) {
                    groupID = valueOps.get(userID);
                    return Long.valueOf(groupID.toString());
                }    
            }
            return null;
        }
    }

    public boolean insertToCountryQueue(String country, String userID) {
        return zSetOps.add(country, userID, new Date().getTime());
    }

    public void removeFromCountryQueue(String country, String userID) {
        zSetOps.remove(country, userID);
    }

    /**
     * Read user-related data from the persistent storage and update the Group Leaderboard hash and User-Group key-value pairs
     * @param userID
     * @return true if cache is updated, false if the user is not a participant of the active tournament
     */
    public boolean loadUserToRedis(String userID) {
        LocalDateTime currentTimeUtc = DateUtil.getCurrentTimeUTC();

        List<Object[]> userDetailsList = userInTournamentRepository.findUserDetailsInTournament(UUID.fromString(userID), currentTimeUtc);
        if (userDetailsList.isEmpty()) { 
            return false;
        }

        Object[] userDetails = userDetailsList.get(0);
        setUserGroup(userDetails[1].toString(), Long.valueOf(userDetails[0].toString()));

        boolean loadSuccess = loadGroupToRedis(Long.valueOf(userDetails[0].toString()));
        return loadSuccess;
    }

    /**
     * Read user-related data from the persistent storage and update the Group Leaderboard hash and User-Group key-value pairs
     * for every user in a group.
     * @param groupID
     * @return true if cache is updated, false if the group does not exists in the active tournament
     */
    public boolean loadGroupToRedis(Long groupID) {
        List<Object[]> usersInGroup = userInTournamentRepository.findUsersInGroup(groupID);

        if (usersInGroup.isEmpty()) {
            return false;
        }

        for (Object[] userDetails: usersInGroup) {
            UserInLeaderboard leaderboardUser = new UserInLeaderboard();
            leaderboardUser.setUser_id(userDetails[1].toString());
            leaderboardUser.setUsername((String) userDetails[2]);
            leaderboardUser.setCountry(userDetails[3].toString());
            leaderboardUser.setScore((Integer) userDetails[4]);
            
            // userDetails[0].toString() is the group ID and userDetails[1].toString() is the user ID
            setUserGroup(userDetails[1].toString(), Long.valueOf(userDetails[0].toString()));
            insertUserInGroupLeaderboard(userDetails[0].toString(), userDetails[1].toString(), leaderboardUser);    
        }
        return true;
    }
    
}
