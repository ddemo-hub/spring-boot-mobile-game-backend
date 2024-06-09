package com.dreamgames.backendengineeringcasestudy.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.entity.UserInTournament;
import com.dreamgames.backendengineeringcasestudy.entity.UserInTournament.UserInTournamentID;

import jakarta.transaction.Transactional;

@Repository
public interface UserInTournamentRepository extends JpaRepository<UserInTournament, UserInTournamentID> {
    
       @Query("SELECT u.country, SUM(uit.score) FROM UserInTournament uit " +
              "JOIN User u ON uit.id.user_id = u.user_id " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
              "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20 " +
              "GROUP BY u.country")
       List<Object[]> findCountryTotalScoresInTournament(@Param("currentTime") LocalDateTime currentTime);

       @Query("SELECT uit.id.group_id, uit.id.user_id, u.username, u.country, uit.score FROM UserInTournament uit " +
              "JOIN User u ON uit.id.user_id = u.user_id " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE uit.id.user_id = :userId " + 
              "AND FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
              "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20")
       List<Object[]> findUserDetailsInTournament(@Param("userId") UUID userId, @Param("currentTime") LocalDateTime currentTime);

       @Query("SELECT uit.id.group_id, uit.id.user_id, u.username, u.country, uit.score FROM UserInTournament uit " +
              "JOIN User u ON uit.id.user_id = u.user_id " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE uit.id.group_id = :groupId ")
       List<Object[]> findUsersInGroup(@Param("groupId") Long groupId);

       @Query("SELECT uit.id.group_id, uit.id.user_id, u.username, u.country, uit.score FROM UserInTournament uit " +
              "JOIN User u ON uit.id.user_id = u.user_id " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
              "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20")
       List<Object[]> findAllUserDetailsInTournament(@Param("currentTime") LocalDateTime currentTime);

       @Modifying
       @Transactional
       @Query("UPDATE UserInTournament uit " +
              "SET uit.score = uit.score + 1 " +
              "WHERE uit.id.user_id = :userId AND uit.id.group_id = :groupId")
       void updateScore(@Param("userId") UUID userId, @Param("groupId") Long groupId);

       @Query("SELECT COUNT(1) + 1 FROM UserInTournament uit " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE uit.id.group_id = " +
              "(SELECT u.id.group_id FROM UserInTournament u " +
              "JOIN TournamentGroup tg2 ON u.id.group_id = tg2.group_id " +
              "WHERE u.id.user_id = :userId AND FUNCTION('DATE', tg2.date_formed) = FUNCTION('DATE', :dateFormed)) " +
              "AND uit.score > " +
              "(SELECT u.score FROM UserInTournament u " +
              "WHERE u.id.user_id = :userId AND u.id.group_id = " +
              "(SELECT u2.id.group_id FROM UserInTournament u2 " +
              "JOIN TournamentGroup tg3 ON u2.id.group_id = tg3.group_id " +
              "WHERE u2.id.user_id = :userId AND FUNCTION('DATE', tg3.date_formed) = FUNCTION('DATE', :dateFormed)))")
       int findUserRankInGroup(@Param("userId") UUID userId, @Param("dateFormed") LocalDateTime dateFormed);
       
       @Query("SELECT uit.id.user_id " +
              "FROM UserInTournament uit " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
              "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20 " +
              "AND uit.score = (SELECT MAX(u.score) FROM UserInTournament u WHERE u.id.group_id = uit.id.group_id)")
       List<UUID> findRankOneUserIDs(@Param("currentTime") LocalDateTime currentTime);
      
       @Query("SELECT uit.id.user_id " +
              "FROM UserInTournament uit " +
              "JOIN TournamentGroup tg ON uit.id.group_id = tg.group_id " +
              "WHERE FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
              "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20 " +
              "AND uit.score = (SELECT MAX(u.score) FROM UserInTournament u WHERE u.id.group_id = uit.id.group_id AND u.score < (SELECT MAX(v.score) FROM UserInTournament v WHERE v.id.group_id = uit.id.group_id))")
       List<UUID> findRankTwoUserIDs(@Param("currentTime") LocalDateTime currentTime);

       @Modifying
       @Transactional
       @Query("UPDATE UserInTournament uit " +
              "SET uit.reward = :reward, uit.is_reward_claimed = false " +
              "WHERE uit.id.user_id IN :userIds")
       void updateRewards(@Param("userIds") List<UUID> userIds, @Param("reward") int reward);

       @Query("SELECT CASE WHEN COUNT(uit) > 0 THEN true ELSE false END " +
              "FROM UserInTournament uit " +
              "WHERE uit.id.user_id = :userId AND uit.is_reward_claimed = false")
       boolean existsUnclaimedRewards(@Param("userId") UUID userId);
       
       @Modifying
       @Transactional
       @Query("UPDATE UserInTournament uit " +
              "SET uit.is_reward_claimed = true " +
              "WHERE uit.id.user_id = :userId")
       void claimReward(@Param("userId") UUID userId);
}
