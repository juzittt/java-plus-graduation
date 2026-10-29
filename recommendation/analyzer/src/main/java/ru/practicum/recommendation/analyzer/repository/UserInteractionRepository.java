package ru.practicum.recommendation.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.recommendation.analyzer.entity.UserInteraction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, UserInteraction.UserInteractionId> {

    List<UserInteraction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    @Query("SELECT ui.eventId FROM UserInteraction ui WHERE ui.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ui.eventId, SUM(ui.maxWeight) FROM UserInteraction ui WHERE ui.eventId IN :eventIds GROUP BY ui.eventId")
    List<Object[]> sumMaxWeightByEventIdIn(@Param("eventIds") List<Long> eventIds);
}