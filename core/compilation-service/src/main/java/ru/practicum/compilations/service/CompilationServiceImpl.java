package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilations.client.EventClient;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.EventShortDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.exception.NotFoundException;

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
        log.info("Saving new compilation: {}", dto);

        Compilation compilation = compilationMapper.toCompilation(dto);
        compilation.setEventIds(dto.getEvents() == null ? new HashSet<>() : new HashSet<>(dto.getEvents()));

        Compilation saved = compilationRepository.save(compilation);
        CompilationDto result = toDto(saved);

        log.info("Saved compilation: {}", result);
        return result;
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
        CompilationDto result = toDto(saved);

        log.info("Updated compilation: {}", result);
        return result;
    }

    @Override
    public void delete(Long compId) {
        log.info("Deleting compilation {}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " not found");
        }
        compilationRepository.deleteById(compId);
        log.info("Deleted compilation {}", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Getting compilations: pinned={}, from={}, size={}", pinned, from, size);
        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations = compilationRepository.findAllByPinned(pinned, pageable);
        List<CompilationDto> dtos = toDtoList(compilations);

        log.info("Got {} compilations", dtos.size());
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Getting compilation by id {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " not found"));

        CompilationDto dto = toDto(compilation);

        log.info("Got compilation: {}", dto);
        return dto;
    }

    private CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = compilationMapper.toCompilationDto(compilation);
        dto.setEvents(fetchEvents(compilation.getEventIds()));
        return dto;
    }

    private List<CompilationDto> toDtoList(List<Compilation> compilations) {
        List<CompilationDto> dtos = compilations.stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());

        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEventIds() == null ? Stream.empty() : c.getEventIds().stream())
                .collect(Collectors.toSet());

        Map<Long, EventShortDto> eventsMap = allEventIds.isEmpty()
                ? Map.of()
                : eventClient.getEventsByIds(new ArrayList<>(allEventIds)).stream()
                .collect(Collectors.toMap(EventShortDto::getId, Function.identity()));

        for (int i = 0; i < compilations.size(); i++) {
            Set<Long> ids = compilations.get(i).getEventIds();
            dtos.get(i).setEvents(ids == null ? List.of() : ids.stream()
                    .map(eventsMap::get)
                    .filter(Objects::nonNull)
                    .toList());
        }

        return dtos;
    }

    private List<EventShortDto> fetchEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return eventClient.getEventsByIds(new ArrayList<>(eventIds));
    }
}