package com.dreamgames.backendengineeringcasestudy.dto;

import java.util.UUID;

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
public class UserResponseDTO {
    
    private UUID userID;
    private String username;
    private long coins;
    private int level;
    private String country;

    private String authToken;

    @Override
    public String toString() {
        return "UserResponseDTO(userID=%s, username=%s, coins=%d, level=%d, country=%s)"
                .formatted(userID, username, coins, level, country);
    }
}
