package ru.practicum.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.dto.EventStatusDto;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventsRepository;
import ru.practicum.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private final EventsRepository eventsRepository;

    @GetMapping("/{id}/status")
    public EventStatusDto getEventStatus(@PathVariable Long id) {
        Event event = eventsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event с id=" + id + " не найден"));

        return EventStatusDto.builder()
                .id(event.getId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .initiatorId(event.getInitiatorId())
                .build();
    }
}