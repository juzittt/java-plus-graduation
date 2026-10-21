package ru.practicum.requests.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.requests.dto.EventRequestsCountDto;
import ru.practicum.requests.repository.ParticipationRequestsRepository;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestsController {

    private final ParticipationRequestsRepository requestRepository;

    @GetMapping("/confirmed-counts")
    public List<EventRequestsCountDto> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return requestRepository.countConfirmedRequestsByEventIds(eventIds);
    }
}