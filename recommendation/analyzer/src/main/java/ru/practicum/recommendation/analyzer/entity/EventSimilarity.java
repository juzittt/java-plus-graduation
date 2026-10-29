package ru.practicum.recommendation.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "event_similarities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EventSimilarity.EventSimilarityId.class)
public class EventSimilarity {

    @Id
    @Column(name = "event_a")
    private Long eventA;

    @Id
    @Column(name = "event_b")
    private Long eventB;

    @Column(name = "score")
    private Double score;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSimilarityId implements Serializable {
        private Long eventA;
        private Long eventB;
    }
}