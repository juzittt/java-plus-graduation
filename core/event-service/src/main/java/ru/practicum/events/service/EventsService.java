package ru.practicum.events.service;

import ru.practicum.events.dto.*;
import ru.practicum.events.dto.params.AdminEventParams;
import ru.practicum.events.dto.params.PaginationParams;
import ru.practicum.events.dto.params.PublicEventParams;
import ru.practicum.events.dto.params.UserEventPathParams;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EventsService {

    List<EventShortDto> getEvents(PublicEventParams params, HttpServletRequest request);

    EventFullDto getEvent(Long id, HttpServletRequest request);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, PaginationParams params, HttpServletRequest request);

    EventFullDto getUserEvent(UserEventPathParams params);

    EventFullDto updateUserEvent(UserEventPathParams params, UpdateEventUserRequest dto);

    List<EventFullDto> getAdminEvents(AdminEventParams params);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);
}
