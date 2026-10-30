package ru.practicum.recommendation.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorListener {

    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    private static final String TOPIC_USER_ACTIONS = "stats.user-actions.v1";
    private static final String TOPIC_SIMILARITY = "stats.events-similarity.v1";

    @KafkaListener(topics = TOPIC_USER_ACTIONS, groupId = "aggregator-group")
    public void onUserAction(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = getWeight(action.getActionType());

        long timestampMs = action.getTimestamp().toEpochMilli();

        Map<Long, Double> userWeights = userEventWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double oldWeight = userWeights.get(userId);

        if (oldWeight != null && newWeight <= oldWeight) {
            log.debug("Weight did not increase for user {} on event {}. Skipping recalculation.", userId, eventId);
            return;
        }

        userWeights.put(userId, newWeight);
        double weightDelta = newWeight - (oldWeight == null ? 0.0 : oldWeight);
        eventTotalWeights.merge(eventId, weightDelta, Double::sum);
        double totalWeightA = eventTotalWeights.get(eventId);

        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) {
                continue;
            }

            Double otherWeight = entry.getValue().get(userId);
            if (otherWeight == null) {
                continue;
            }

            double oldMinForPair = Math.min(oldWeight == null ? 0.0 : oldWeight, otherWeight);
            double newMinForPair = Math.min(newWeight, otherWeight);
            double deltaMin = newMinForPair - oldMinForPair;

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            Map<Long, Double> pairSums = minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>());
            pairSums.merge(second, deltaMin, Double::sum);
            double currentMinSum = pairSums.get(second);

            double totalWeightB = eventTotalWeights.get(otherEventId);
            double denominator = Math.sqrt(totalWeightA) * Math.sqrt(totalWeightB);
            double similarity = denominator > 0 ? (currentMinSum / Math.sqrt(totalWeightA * totalWeightB)) : 0.0;

            EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(similarity)
                    .setTimestamp(Instant.ofEpochMilli(timestampMs))
                    .build();

            kafkaTemplate.send(TOPIC_SIMILARITY, first, similarityAvro)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send similarity to Kafka for events {} and {}", first, second, ex);
                        }
                    });
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> WEIGHT_VIEW;
            case REGISTER -> WEIGHT_REGISTER;
            case LIKE -> WEIGHT_LIKE;
        };
    }
}