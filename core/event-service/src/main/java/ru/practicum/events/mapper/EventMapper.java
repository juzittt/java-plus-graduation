package ru.practicum.events.mapper;

import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "eventDate", ignore = true)
    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "initiatorId", source = "initiator")
    @Mapping(target = "location", source = "dto.location")
    Event toEvent(NewEventDto dto);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "initiator.id", source = "initiatorId")
    @Mapping(target = "category.id", source = "categoryId")
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "initiator.id", source = "initiatorId")
    @Mapping(target = "category.id", source = "categoryId")
    EventShortDto toEventShortDto(Event event);

    List<EventFullDto> toEventFullDtoList(List<Event> events);
    List<EventShortDto> toEventShortDtoList(List<Event> events);

    LocationDto toLocationDto(Location location);

    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto dto);
}