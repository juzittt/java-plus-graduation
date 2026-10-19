package ru.practicum.compilations.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.compilations.dto.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/by-ids")
    List<EventShortDto> getEventsByIds(@RequestParam("ids") List<Long> ids);
}