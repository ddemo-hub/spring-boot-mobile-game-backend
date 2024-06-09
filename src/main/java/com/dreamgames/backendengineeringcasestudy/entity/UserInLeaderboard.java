package com.dreamgames.backendengineeringcasestudy.entity;

import java.io.Serializable;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash 
public class UserInLeaderboard implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private String user_id;
    
    private String username;

    private String country;

    private int score = 0;

    public void increment() {
        this.score++;
    }
}
