package ru.practicum.recommendation.analyzer.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.stats.grpc.*;
import ru.practicum.recommendation.analyzer.entity.EventSimilarity;
import ru.practicum.recommendation.analyzer.entity.UserInteraction;
import ru.practicum.recommendation.analyzer.repository.EventSimilarityRepository;
import ru.practicum.recommendation.analyzer.repository.UserInteractionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        List<Long> eventIds = request.getEventIdsList();
        log.info("GetInteractionsCount for events: {}", eventIds);

        List<Object[]> results = userInteractionRepository.sumMaxWeightByEventIdIn(eventIds);

        for (Object[] row : results) {
            Long eventId = (Long) row[0];
            Double totalWeight = (Double) row[1];

            double roundedScore = BigDecimal.valueOf(totalWeight)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(roundedScore)
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("GetSimilarEvents: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventBOrderByScoreDesc(eventId);

        Set<Long> userEventIds = userInteractionRepository.findEventIdsByUserId(userId);

        int count = 0;
        for (EventSimilarity similarity : similarities) {
            Long similarEventId = similarity.getEventA().equals(eventId)
                    ? similarity.getEventB()
                    : similarity.getEventA();

            if (userEventIds.contains(similarEventId)) {
                continue;
            }

            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(similarEventId)
                    .setScore(similarity.getScore())
                    .build();
            responseObserver.onNext(proto);

            count++;
            if (count >= maxResults) {
                break;
            }
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("GetRecommendationsForUser: userId={}, maxResults={}", userId, maxResults);

        List<UserInteraction> recentInteractions = userInteractionRepository.findByUserIdOrderByTimestampDesc(
                userId, PageRequest.of(0, maxResults)
        );

        if (recentInteractions.isEmpty()) {
            log.info("No interactions found for user {}", userId);
            responseObserver.onCompleted();
            return;
        }

        Set<Long> userEventIds = recentInteractions.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> userWeights = recentInteractions.stream()
                .collect(Collectors.toMap(UserInteraction::getEventId, UserInteraction::getMaxWeight));

        Map<Long, Double> candidateScores = new HashMap<>();

        for (Long userEventId : userEventIds) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventBOrderByScoreDesc(userEventId);

            for (EventSimilarity similarity : similarities) {
                Long candidateId = similarity.getEventA().equals(userEventId)
                        ? similarity.getEventB()
                        : similarity.getEventA();

                if (userEventIds.contains(candidateId)) {
                    continue;
                }

                double score = similarity.getScore();
                double weight = userWeights.get(userEventId);

                candidateScores.merge(candidateId, score * weight, Double::sum);
            }
        }

        List<Map.Entry<Long, Double>> sortedCandidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .toList();

        for (Map.Entry<Long, Double> entry : sortedCandidates) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(entry.getKey())
                    .setScore(entry.getValue())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
    }
}