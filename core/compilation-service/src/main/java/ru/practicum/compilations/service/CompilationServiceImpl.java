package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.compilations.client.EventClient;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventClient eventClient;

    @Override
    public CompilationDto save(NewCompilationDto dto) {
        log.info("Saving new compilation: {}", dto.getTitle());

        Compilation compilation = compilationMapper.toCompilation(dto);
        compilation.setEventIds(dto.getEvents() == null
                ? new HashSet<>()
                : new HashSet<>(dto.getEvents()));

        Compilation saved = compilationRepository.save(compilation);
        return enrichWithEvents(compilationMapper.toCompilationDto(saved), saved.getEventIds());
    }

    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        log.info("Updating compilation {}: {}", compId, dto);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " not found"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            compilation.setEventIds(new HashSet<>(dto.getEvents()));
        }

        Compilation saved = compilationRepository.save(compilation);
        return enrichWithEvents(compilationMapper.toCompilationDto(saved), saved.getEventIds());
    }

    @Override
    public void delete(Long compId) {
        log.info("Deleting compilation {}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Getting compilations: pinned={}, from={}, size={}", pinned, from, size);
        Pageable pageable = PageRequest.of(from / Math.max(size, 1), Math.max(size, 1));

        List<Compilation> compilations = (pinned == null)
                ? compilationRepository.findAllBy(pageable)
                : compilationRepository.findAllByPinned(pinned, pageable);

        return enrichListWithEvents(compilations);
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Getting compilation by id {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " not found"));

        return enrichWithEvents(compilationMapper.toCompilationDto(compilation), compilation.getEventIds());
    }

    private CompilationDto enrichWithEvents(CompilationDto dto, Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            dto.setEvents(List.of());
            return dto;
        }

        try {
            List<EventShortDto> events = eventClient.getEventsByIds(new ArrayList<>(eventIds));
            Map<Long, EventShortDto> map = events.stream()
                    .collect(Collectors.toMap(EventShortDto::getId, Function.identity(), (a, b) -> a));

            dto.setEvents(eventIds.stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .toList());
        } catch (Exception e) {
            log.warn("Degraded mode: event-service unavailable for compilation {}", dto.getId());
            dto.setEvents(List.of());
        }
        return dto;
    }

    private List<CompilationDto> enrichListWithEvents(List<Compilation> compilations) {
        List<CompilationDto> dtos = compilations.stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());

        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEventIds() == null ? Stream.empty() : c.getEventIds().stream())
                .collect(Collectors.toSet());

        Map<Long, EventShortDto> eventsMap;
        if (allEventIds.isEmpty()) {
            eventsMap = Map.of();
        } else {
            try {
                eventsMap = eventClient.getEventsByIds(new ArrayList<>(allEventIds)).stream()
                        .collect(Collectors.toMap(EventShortDto::getId, Function.identity(), (a, b) -> a));
            } catch (Exception e) {
                log.warn("Degraded mode: event-service unavailable, returning compilations without events");
                eventsMap = Map.of();
            }
        }

        for (int i = 0; i < compilations.size(); i++) {
            Set<Long> ids = compilations.get(i).getEventIds();
            dtos.get(i).setEvents(ids == null ? List.of() : ids.stream()
                    .map(eventsMap::get)
                    .filter(Objects::nonNull)
                    .toList());
        }

        return dtos;
    }
}