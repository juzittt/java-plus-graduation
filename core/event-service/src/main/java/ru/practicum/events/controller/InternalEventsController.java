package ru.practicum.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.EventStatusDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventsRepository;
import ru.practicum.events.service.EventsServiceImpl;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private final EventsRepository eventsRepository;
    private final EventMapper eventMapper;
    private final EventsServiceImpl eventsService;

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

    @GetMapping("/by-ids")
    public List<EventShortDto> getEventsByIds(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventsRepository.findAllById(ids);
        List<EventShortDto> dtos = eventMapper.toEventShortDtoList(events);
        eventsService.enrichShortWithUsersAndCategories(dtos);

        return dtos;
    }
}