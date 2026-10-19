package ru.practicum.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusDto {
    private Long id;
    private String state;
    private Integer participantLimit;
    private Boolean requestModeration;
    private Long initiatorId;
}