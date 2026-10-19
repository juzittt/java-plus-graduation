package ru.practicum.requests.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.requests.dto.EventStatusDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{id}/status")
    EventStatusDto getEventStatus(@PathVariable("id") Long eventId);
}