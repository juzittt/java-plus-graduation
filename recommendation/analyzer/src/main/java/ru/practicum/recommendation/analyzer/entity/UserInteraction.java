package ru.practicum.recommendation.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserInteraction.UserInteractionId.class)
public class UserInteraction {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "max_weight")
    private Double maxWeight;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInteractionId implements Serializable {
        private Long userId;
        private Long eventId;
    }
}