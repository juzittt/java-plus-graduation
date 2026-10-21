package ru.practicum.requests.dto;

import ru.practicum.requests.model.ParticipationStatus;
import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    List<Long> requestIds;
    ParticipationStatus status;
}
