package ru.practicum.events.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.CategoryClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.StatsClient;
import ru.practicum.client.UserClient;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ValidationException;
import ru.practicum.events.dto.*;
import ru.practicum.events.dto.params.AdminEventParams;
import ru.practicum.events.dto.params.PaginationParams;
import ru.practicum.events.dto.params.PublicEventParams;
import ru.practicum.events.dto.params.UserEventPathParams;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.State;
import ru.practicum.events.repository.EventsRepository;
import ru.practicum.events.repository.specification.EventSpecification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventsServiceImpl implements EventsService {

    private final EventsRepository eventsRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final ObjectMapper objectMapper;
    private final RequestClient requestClient;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final String APP_NAME = "ewm-event-service";

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(PublicEventParams params, HttpServletRequest request) {
        LocalDateTime startDate = params.getRangeStart();
        LocalDateTime endDate = params.getRangeEnd();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("rangeStart must be before rangeEnd");
        }

        sendHitToStats("/events", request.getRemoteAddr());

        Specification<Event> spec = Specification
                .where(EventSpecification.published())
                .and(EventSpecification.hasText(params.getText()))
                .and(EventSpecification.hasCategories(params.getCategories()))
                .and(EventSpecification.hasPaid(params.getPaid()))
                .and(EventSpecification.dateAfter(startDate))
                .and(EventSpecification.dateBefore(endDate));

        List<Long> availableEventIds = null;
        if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
            List<Event> candidateEvents = eventsRepository.findAll(spec);
            availableEventIds = filterAvailableEvents(candidateEvents);
        }

        spec = spec.and(EventSpecification.onlyAvailable(params.getOnlyAvailable(), availableEventIds));

        return switch (params.getSort()) {
            case VIEWS -> getEventsSortedByViews(spec, params);
            case EVENT_DATE -> getEventsSortedByDate(spec, params);
            case null -> getEventsSortedByDate(spec, params);
        };
    }

    private List<Long> filterAvailableEvents(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedCounts = requestClient.getConfirmedCounts(eventIds).stream()
                .collect(Collectors.toMap(
                        EventRequestsCountDto::eventId,
                        EventRequestsCountDto::confirmedRequests,
                        (a, b) -> a
                ));

        return events.stream()
                .filter(event -> {
                    Integer limit = event.getParticipantLimit();
                    if (limit == null || limit == 0) {
                        return true;
                    }
                    long confirmed = confirmedCounts.getOrDefault(event.getId(), 0L);
                    return confirmed < limit;
                })
                .map(Event::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(AdminEventParams params) {
        PageRequest pageRequest = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by(Sort.Direction.DESC, "eventDate")
        );

        List<State> states = params.getStates() == null
                ? null
                : params.getStates().stream().map(State::valueOf).toList();

        Specification<Event> spec = Specification
                .where(EventSpecification.hasUser(params.getUsers()))
                .and(EventSpecification.hasStates(states))
                .and(EventSpecification.hasCategories(params.getCategories()))
                .and(EventSpecification.dateAfter(params.getRangeStart()))
                .and(EventSpecification.dateBefore(params.getRangeEnd()));

        Page<Event> page = eventsRepository.findAll(spec, pageRequest);
        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventFullDto> dtos = eventMapper.toEventFullDtoList(events);
        enrichWithUsersAndCategories(dtos);
        enrichEventFullDtosWithStats(dtos);

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEvent(Long id, HttpServletRequest request) {
        Event event = eventsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event с id=" + id + " не найден"));

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Event с id=" + id + " не найден");
        }

        sendHitToStats("/events/" + id, request.getRemoteAddr());

        EventFullDto dto = eventMapper.toEventFullDto(event);
        enrichEventWithUserAndCategory(dto);
        enrichEventWithStats(dto);

        return dto;
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        categoryClient.getCategory(dto.getCategory());

         userClient.getUserById(userId);

        LocalDateTime eventDate = parseAndValidateEventDate(
                dto.getEventDate(),
                "Дата события должна быть не ранее чем через 2 часа от текущего момента"
        );

        Event event = eventMapper.toEvent(dto);
        event.setCategoryId(dto.getCategory());
        event.setInitiatorId(userId);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);
        event.setEventDate(eventDate);

        event = eventsRepository.save(event);

        EventFullDto result = eventMapper.toEventFullDto(event);
        enrichEventWithUserAndCategory(result);
        result.setViews(0L);
        result.setConfirmedRequests(0L);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(UserEventPathParams params) {
        Event event = eventsRepository.findByIdAndInitiatorId(params.getEventId(), params.getUserId())
                .orElseThrow(() -> new NotFoundException("Event с id=" + params.getEventId() + " не найден"));

        EventFullDto dto = eventMapper.toEventFullDto(event);
        enrichEventWithUserAndCategory(dto);
        enrichEventWithStats(dto);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, PaginationParams params, HttpServletRequest request) {
        userClient.getUserById(userId);

        sendHitToStats("/users/" + userId + "/events", request.getRemoteAddr());

        PageRequest pageRequest = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by(Sort.Direction.DESC, "eventDate")
        );

        Page<Event> page = eventsRepository.findByInitiatorId(userId, pageRequest);
        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> dtos = eventMapper.toEventShortDtoList(events);
        enrichShortWithUsersAndCategories(dtos);
        enrichEventsWithStats(dtos);

        return dtos;
    }

    @Override
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event с id=" + eventId + " не найден"));

        updateAdminEventFields(event, request);
        handleAdminStateAction(event, request.getStateAction());

        event = eventsRepository.save(event);

        EventFullDto result = eventMapper.toEventFullDto(event);
        enrichEventWithUserAndCategory(result);
        enrichEventWithStats(result);

        return result;
    }

    @Override
    public EventFullDto updateUserEvent(UserEventPathParams params, UpdateEventUserRequest dto) {
        Event event = eventsRepository.findByIdAndInitiatorId(params.getEventId(), params.getUserId())
                .orElseThrow(() -> new NotFoundException("Event с id=" + params.getEventId() + " не найден"));

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Можно изменять только отмененные события или события в состоянии ожидания модерации");
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = parseAndValidateEventDate(
                    dto.getEventDate(),
                    "Дата события должна быть не ранее чем через 2 часа от текущего момента"
            );
            event.setEventDate(eventDate);
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());

        if (dto.getCategory() != null) {
            categoryClient.getCategory(dto.getCategory());
            event.setCategoryId(dto.getCategory());
        }

        if (dto.getLocation() != null) {
            event.getLocation().setLat(dto.getLocation().getLat());
            event.getLocation().setLon(dto.getLocation().getLon());
        }

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(State.PENDING);
                case CANCEL_REVIEW -> event.setState(State.CANCELED);
                default -> throw new ValidationException("Неизвестное состояние: " + dto.getStateAction());
            }
        }

        event = eventsRepository.save(event);

        EventFullDto result = eventMapper.toEventFullDto(event);
        enrichEventWithUserAndCategory(result);
        enrichEventWithStats(result);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventStatusDto getEventStatus(Long id) {
        Event event = eventsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event с id=" + id + " не найден"));

        return EventStatusDto.builder()
                .id(event.getId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .initiatorId(event.getInitiatorId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getShortDtosByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventsRepository.findAllById(ids);
        List<EventShortDto> dtos = eventMapper.toEventShortDtoList(events);
        enrichShortWithUsersAndCategories(dtos);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCategoryId(Long categoryId) {
        return eventsRepository.existsByCategoryId(categoryId);
    }

    private void sendHitToStats(String uri, String ip) {
        statsClient.saveHit(EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private Map<Long, Long> getViewsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();

        ResponseEntity<Object> response = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                true
        );

        if (response.getBody() == null) {
            return Collections.emptyMap();
        }

        List<ViewStatsDto> stats = objectMapper.convertValue(
                response.getBody(),
                new TypeReference<List<ViewStatsDto>>() {}
        );

        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().substring(stat.getUri().lastIndexOf('/') + 1)),
                        ViewStatsDto::getHits,
                        (existing, replacement) -> existing
                ));
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return requestClient.getConfirmedCounts(eventIds).stream()
                .collect(Collectors.toMap(
                        EventRequestsCountDto::eventId,
                        EventRequestsCountDto::confirmedRequests
                ));
    }

    private void enrichEventWithStats(EventFullDto dto) {
        Map<Long, Long> viewsMap = getViewsMap(List.of(dto.getId()));
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(List.of(dto.getId()));

        dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
        dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L));
    }

    private void enrichEventsWithStats(List<EventShortDto> dtos) {
        if (dtos.isEmpty()) return;

        List<Long> eventIds = dtos.stream().map(EventShortDto::getId).toList();
        Map<Long, Long> viewsMap = getViewsMap(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);

        dtos.forEach(dto -> {
            dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
            dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L));
        });
    }

    private void enrichEventFullDtosWithStats(List<EventFullDto> dtos) {
        if (dtos.isEmpty()) return;

        List<Long> eventIds = dtos.stream().map(EventFullDto::getId).toList();
        Map<Long, Long> viewsMap = getViewsMap(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);

        dtos.forEach(dto -> {
            dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
            dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L));
        });
    }

    private void enrichWithUsersAndCategories(List<EventFullDto> dtos) {
        Set<Long> userIds = dtos.stream().map(dto -> dto.getInitiator().getId()).collect(Collectors.toSet());
        Set<Long> categoryIds = dtos.stream().map(dto -> dto.getCategory().getId()).collect(Collectors.toSet());

        Map<Long, UserShortDto> users = userIds.isEmpty() ? Map.of() :
                userClient.getUsersByIds(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        Map<Long, CategoryDto> categories = categoryIds.isEmpty() ? Map.of() :
                categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds)).stream()
                        .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        dtos.forEach(dto -> {
            dto.setInitiator(users.getOrDefault(dto.getInitiator().getId(), new UserShortDto(dto.getInitiator().getId(), "Unknown")));
            dto.setCategory(categories.getOrDefault(dto.getCategory().getId(), new CategoryDto(dto.getCategory().getId(), "Unknown")));
        });
    }

    public void enrichShortWithUsersAndCategories(List<EventShortDto> dtos) {
        Set<Long> userIds = dtos.stream().map(dto -> dto.getInitiator().getId()).collect(Collectors.toSet());
        Set<Long> categoryIds = dtos.stream().map(dto -> dto.getCategory().getId()).collect(Collectors.toSet());

        Map<Long, UserShortDto> users = userIds.isEmpty() ? Map.of() :
                userClient.getUsersByIds(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        Map<Long, CategoryDto> categories = categoryIds.isEmpty() ? Map.of() :
                categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds)).stream()
                        .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        dtos.forEach(dto -> {
            dto.setInitiator(users.getOrDefault(dto.getInitiator().getId(), new UserShortDto(dto.getInitiator().getId(), "Unknown")));
            dto.setCategory(categories.getOrDefault(dto.getCategory().getId(), new CategoryDto(dto.getCategory().getId(), "Unknown")));
        });
    }

    private void enrichEventWithUserAndCategory(EventFullDto dto) {
        UserShortDto user = userClient.getUserById(dto.getInitiator().getId());
        CategoryDto category = categoryClient.getCategory(dto.getCategory().getId());
        dto.setInitiator(user);
        dto.setCategory(category);
    }

    private LocalDateTime parseAndValidateEventDate(String dateStr, String errorMessage) {
        LocalDateTime eventDate;
        try {
            eventDate = LocalDateTime.parse(dateStr, FORMATTER);
        } catch (Exception e) {
            throw new ValidationException("Неверный формат даты. Ожидается формат: " + DATE_FORMAT);
        }

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(errorMessage);
        }

        return eventDate;
    }

    private void updateAdminEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getEventDate() != null) {
            LocalDateTime eventDate = parseAndValidateEventDate(
                    request.getEventDate(),
                    "Дата события не может быть в прошлом"
            );

            if (event.getState() == State.PUBLISHED && eventDate.isBefore(event.getPublishedOn().plusHours(1))) {
                throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
            }

            event.setEventDate(eventDate);
        }

        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());

        if (request.getCategory() != null) {
            categoryClient.getCategory(request.getCategory());
            event.setCategoryId(request.getCategory());
        }

        if (request.getLocation() != null) {
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }
    }

    private void handleAdminStateAction(Event event, StateAction stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case PUBLISH_EVENT -> {
                if (event.getState() != State.PENDING) {
                    throw new ConflictException("Событие должно находиться в состоянии ожидания публикации");
                }
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            }
            case REJECT_EVENT -> {
                if (event.getState() == State.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
                event.setState(State.CANCELED);
            }
            default -> throw new ValidationException("Неизвестное состояние: " + stateAction);
        }
    }

    private List<EventShortDto> getEventsSortedByDate(Specification<Event> spec, PublicEventParams params) {
        PageRequest pageRequest = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        Page<Event> page = eventsRepository.findAll(spec, pageRequest);
        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> dtos = eventMapper.toEventShortDtoList(events);
        enrichShortWithUsersAndCategories(dtos);
        enrichEventsWithStats(dtos);

        return dtos;
    }

    private List<EventShortDto> getEventsSortedByViews(Specification<Event> spec, PublicEventParams params) {
        List<Event> allEvents = eventsRepository.findAll(spec);

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> dtos = eventMapper.toEventShortDtoList(allEvents);
        enrichShortWithUsersAndCategories(dtos);
        enrichEventsWithStats(dtos);

        dtos.sort(Comparator.comparing(EventShortDto::getViews).reversed());

        int from = params.getFrom();
        int size = params.getSize();

        if (from >= dtos.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(from + size, dtos.size());
        return dtos.subList(from, toIndex);
    }
}