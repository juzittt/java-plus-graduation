package ru.practicum.events.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.Location;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "eventDate", ignore = true)
    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "initiatorId", ignore = true)
    Event toEvent(NewEventDto dto);

    @Mapping(target = "initiator.id", source = "initiatorId")
    @Mapping(target = "category.id", source = "categoryId")
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "initiator.id", source = "initiatorId")
    @Mapping(target = "category.id", source = "categoryId")
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    EventShortDto toEventShortDto(Event event);

    List<EventFullDto> toEventFullDtoList(List<Event> events);

    List<EventShortDto> toEventShortDtoList(List<Event> events);

    LocationDto toLocationDto(Location location);

    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto dto);
}