package ru.practicum.recommendation.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.recommendation.analyzer.entity.EventSimilarity;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarity.EventSimilarityId> {

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId ORDER BY es.score DESC")
    List<EventSimilarity> findByEventAOrEventBOrderByScoreDesc(@Param("eventId") Long eventId);
}