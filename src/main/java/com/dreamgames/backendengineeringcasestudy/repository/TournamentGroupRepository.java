package com.dreamgames.backendengineeringcasestudy.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.entity.TournamentGroup;

@Repository
public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, Long> {
    @Query("SELECT tg.group_id " +
           "FROM TournamentGroup tg " +
           "JOIN UserInTournament uit ON tg.group_id = uit.id.group_id " +
           "WHERE uit.id.user_id = :userId " +
           "AND FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :currentTime) " +
           "AND FUNCTION('HOUR', :currentTime) BETWEEN 0 AND 20")
    Optional<Long> getCurrentTournamentGroupID(@Param("userId") UUID userId, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT tg.group_id " +
            "FROM TournamentGroup tg " +
            "JOIN UserInTournament uit ON tg.group_id = uit.id.group_id " +
            "WHERE uit.id.user_id = :userId " +
            "AND FUNCTION('DATE', tg.date_formed) = FUNCTION('DATE', :dateFormed)")
    Optional<Long> getTournamentGroupIDByDate(@Param("userId") UUID userId, @Param("dateFormed") LocalDateTime dateFormed);

}
