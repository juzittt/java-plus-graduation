package ru.practicum.compilations.client;

import org.springframework.stereotype.Component;
import ru.practicum.common.dto.EventShortDto;

import java.util.List;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> ids) {
        return List.of();
    }
}