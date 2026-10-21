package ru.practicum.client;

import org.springframework.stereotype.Component;
import ru.practicum.events.dto.EventRequestsCountDto;

import java.util.List;

@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<EventRequestsCountDto> getConfirmedCounts(List<Long> eventIds) {
        return List.of();
    }
}