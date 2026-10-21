package ru.practicum.compilations.dto;

import lombok.Data;
import ru.practicum.common.dto.EventShortDto;

import java.util.List;

@Data
public class CompilationDto {

    private Long id;
    private String title;
    private Boolean pinned;
    private List<EventShortDto> events;
}
