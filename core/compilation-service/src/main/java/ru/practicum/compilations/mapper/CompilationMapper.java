package ru.practicum.compilations.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.EventShortDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.model.Compilation;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventIds", ignore = true)
    Compilation toCompilation(NewCompilationDto dto);

    @Mapping(target = "events", source = "eventIds")
    CompilationDto toCompilationDto(Compilation compilation);

    default List<EventShortDto> mapIdsToStubDtos(Set<Long> eventIds) {
        if (eventIds == null) {
            return List.of();
        }
        return eventIds.stream()
                .map(id -> EventShortDto.builder().id(id).build())
                .toList();
    }
}