package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.events.dto.EventRequestsCountDto;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/confirmed-counts")
    List<EventRequestsCountDto> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);
}