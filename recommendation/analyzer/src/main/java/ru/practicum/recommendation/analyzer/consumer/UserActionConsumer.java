package ru.practicum.recommendation.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.recommendation.analyzer.entity.UserInteraction;
import ru.practicum.recommendation.analyzer.repository.UserInteractionRepository;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserInteractionRepository userInteractionRepository;

    private static final String TOPIC_USER_ACTIONS = "stats.user-actions.v1";

    @KafkaListener(topics = TOPIC_USER_ACTIONS, groupId = "analyzer-group")
    public void onUserAction(UserActionAvro action) {
        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        Double newWeight = getWeight(action.getActionType());
        Instant timestamp = action.getTimestamp();

        log.info("Received user action: userId={}, eventId={}, weight={}", userId, eventId, newWeight);

        Optional<UserInteraction> existing = userInteractionRepository.findByUserIdAndEventId(userId, eventId);

        if (existing.isPresent()) {
            UserInteraction interaction = existing.get();
            if (newWeight > interaction.getMaxWeight()) {
                interaction.setMaxWeight(newWeight);
                interaction.setTimestamp(timestamp);
                userInteractionRepository.save(interaction);
                log.info("Updated interaction: userId={}, eventId={}, newWeight={}", userId, eventId, newWeight);
            }
        } else {
            UserInteraction interaction = new UserInteraction(userId, eventId, newWeight, timestamp);
            userInteractionRepository.save(interaction);
            log.info("Created interaction: userId={}, eventId={}, weight={}", userId, eventId, newWeight);
        }
    }

    private Double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1.0;
            case REGISTER -> 3.0;
            case LIKE -> 5.0;
        };
    }
}