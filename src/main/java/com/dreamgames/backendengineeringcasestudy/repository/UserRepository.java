package com.dreamgames.backendengineeringcasestudy.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.entity.User;

import jakarta.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.level = u.level + 1, u.coins = u.coins + 25 WHERE u.user_id = :user_id")
    int updateUserLevel(@Param("user_id") UUID userID);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.coins = u.coins - 1000 WHERE u.user_id IN :userIds")
    void collectEntranceFeeByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT u.level AS level, u.coins AS coins FROM User u WHERE u.user_id = :user_id")
    Map<String, Object> findLevelAndCoinsByUserId(@Param("user_id") UUID userId);

    @Query("SELECT u.country as country FROM User u WHERE u.user_id = :user_id")
    Optional<User> findByUserID(@Param("user_id") UUID user_id);

    @Query("SELECT u FROM User u WHERE u.user_id IN :user_ids")
    List<User> findByUserIDs(@Param("user_ids") List<UUID> userIds);
}
