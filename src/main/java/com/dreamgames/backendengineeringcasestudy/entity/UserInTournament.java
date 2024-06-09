package com.dreamgames.backendengineeringcasestudy.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_in_tournament")
public class UserInTournament implements Serializable {
    
    @EmbeddedId
    private UserInTournamentID id;

    private int score = 0;

    private Integer reward = 0;

    private Boolean is_reward_claimed = true;

    @PrePersist
    @PreUpdate
    public void validate() {
        if ((reward == null && is_reward_claimed != null) || (reward != null && is_reward_claimed == null)) {
            throw new IllegalStateException("Either both reward and is_reward_claimed must be null or neither can be null");
        }
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInTournamentID implements Serializable {
        
        private Long group_id;

        private UUID user_id;
    }
}
