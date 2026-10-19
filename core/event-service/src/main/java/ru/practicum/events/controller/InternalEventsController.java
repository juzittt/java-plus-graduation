package ru.practicum.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.EventStatusDto;
import ru.practicum.events.service.EventsService;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private final EventsService eventsService;

    @GetMapping("/{id}/status")
    public EventStatusDto getEventStatus(@PathVariable Long id) {
        return eventsService.getEventStatus(id);
    }

    @GetMapping("/by-ids")
    public List<EventShortDto> getEventsByIds(@RequestParam("ids") List<Long> ids) {
        return eventsService.getShortDtosByIds(ids);
    }

    @GetMapping("/exists-by-category/{categoryId}")
    public boolean existsByCategoryId(@PathVariable Long categoryId) {
        return eventsService.existsByCategoryId(categoryId);
    }
}