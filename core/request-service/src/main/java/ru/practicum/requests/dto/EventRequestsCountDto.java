package ru.practicum.requests.dto;

public record EventRequestsCountDto(
        Long eventId,
        Long confirmedRequests
) {
}
