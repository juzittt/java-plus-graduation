package ru.practicum.events.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.events.dto.EventStatusDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.dto.UpdateEventUserRequest;
import ru.practicum.events.dto.params.AdminEventParams;
import ru.practicum.events.dto.params.PaginationParams;
import ru.practicum.events.dto.params.PublicEventParams;
import ru.practicum.events.dto.params.UserEventPathParams;

import java.util.List;

public interface EventsService {

    List<EventShortDto> getEvents(PublicEventParams params, HttpServletRequest request);

    EventFullDto getEvent(Long id);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, PaginationParams params, HttpServletRequest request);

    EventFullDto getUserEvent(UserEventPathParams params);

    EventFullDto updateUserEvent(UserEventPathParams params, UpdateEventUserRequest dto);

    List<EventFullDto> getAdminEvents(AdminEventParams params);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    EventStatusDto getEventStatus(Long id);

    List<EventShortDto> getShortDtosByIds(List<Long> ids);

    boolean existsByCategoryId(Long categoryId);

    List<EventFullDto> getEventsByIds(List<Long> ids);
}