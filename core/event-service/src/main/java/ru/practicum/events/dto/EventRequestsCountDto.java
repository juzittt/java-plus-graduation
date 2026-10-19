package ru.practicum.events.dto;

public record EventRequestsCountDto(
        Long eventId,
        Long confirmedRequests
) {
}
