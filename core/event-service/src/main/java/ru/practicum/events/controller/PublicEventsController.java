package ru.practicum.events.controller;

import client.StatsAnalyzerClient;
import client.StatsCollectorClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.ValidationException;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventSort;
import ru.practicum.events.dto.params.PublicEventParams;
import ru.practicum.events.service.EventsService;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventsController {

    private final EventsService eventsService;
    private final StatsCollectorClient statsCollectorClient;
    private final StatsAnalyzerClient statsAnalyzerClient;

    @GetMapping("/events")
    public ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSort sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size,
            HttpServletRequest request
    ) {
        log.info("GET /events: text={}, categories={}, paid={}", text, categories, paid);

        PublicEventParams searchParams = PublicEventParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return ResponseEntity.ok(eventsService.getEvents(searchParams, request));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventFullDto> getEvent(
            @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        statsCollectorClient.sendAction(userId, id, ActionTypeProto.ACTION_VIEW, LocalDateTime.now());

        EventFullDto eventDto = eventsService.getEvent(id);

        return ResponseEntity.ok(eventDto);
    }

    @GetMapping("/events/recommendations")
    public ResponseEntity<List<EventFullDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Positive Integer maxResults) {

        List<RecommendedEventProto> recommendations = statsAnalyzerClient.getRecommendationsForUser(userId, maxResults);

        if (recommendations.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toList());

        List<EventFullDto> events = eventsService.getEventsByIds(eventIds);

        return ResponseEntity.ok(events);
    }

    @PutMapping("/events/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        List<RecommendedEventProto> interactions = statsAnalyzerClient.getInteractionsCount(List.of(eventId));

        boolean hasInteracted = !interactions.isEmpty() && interactions.get(0).getScore() > 0.0;

        if (!hasInteracted) {
            throw new ValidationException("Пользователь может лайкать только посещённые им мероприятия");
        }

        statsCollectorClient.sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE, LocalDateTime.now());

        return ResponseEntity.ok().build();
    }
}