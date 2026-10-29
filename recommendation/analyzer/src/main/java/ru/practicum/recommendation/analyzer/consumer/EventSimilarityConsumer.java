package ru.practicum.recommendation.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.recommendation.analyzer.entity.EventSimilarity;
import ru.practicum.recommendation.analyzer.repository.EventSimilarityRepository;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final EventSimilarityRepository eventSimilarityRepository;

    private static final String TOPIC_SIMILARITY = "stats.events-similarity.v1";

    @KafkaListener(topics = TOPIC_SIMILARITY, groupId = "analyzer-group")
    public void onEventSimilarity(EventSimilarityAvro similarity) {
        Long eventA = similarity.getEventA();
        Long eventB = similarity.getEventB();
        Double score = similarity.getScore();
        Instant timestamp = similarity.getTimestamp();

        log.info("Received similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);

        Optional<EventSimilarity> existing = eventSimilarityRepository.findById(
                new EventSimilarity.EventSimilarityId(eventA, eventB)
        );

        if (existing.isPresent()) {
            EventSimilarity similarityEntity = existing.get();
            similarityEntity.setScore(score);
            similarityEntity.setTimestamp(timestamp);
            eventSimilarityRepository.save(similarityEntity);
            log.info("Updated similarity: eventA={}, eventB={}, newScore={}", eventA, eventB, score);
        } else {
            EventSimilarity similarityEntity = new EventSimilarity(eventA, eventB, score, timestamp);
            eventSimilarityRepository.save(similarityEntity);
            log.info("Created similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
        }
    }
}