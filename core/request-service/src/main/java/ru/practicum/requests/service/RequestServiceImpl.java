package ru.practicum.requests.service;

import client.StatsCollectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.requests.client.EventClient;
import ru.practicum.requests.dto.*;
import ru.practicum.requests.mapper.ParticipationRequestMapper;
import ru.practicum.requests.model.ParticipationRequest;
import ru.practicum.requests.model.ParticipationStatus;
import ru.practicum.requests.repository.ParticipationRequestsRepository;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestsRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;
    private final EventClient eventClient;
    private final StatsCollectorClient statsCollectorClient;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        EventStatusDto eventStatus = eventClient.getEventStatus(eventId);

        if (!"PUBLISHED".equals(eventStatus.getState())) {
            throw new ConflictException("Событие еще не опубликовано");
        }

        if (eventStatus.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос уже существует");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
        if (eventStatus.getParticipantLimit() != null && eventStatus.getParticipantLimit() > 0
                && confirmedCount >= eventStatus.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников на данное событие");
        }

        ParticipationStatus status = ParticipationStatus.PENDING;
        if (eventStatus.getParticipantLimit() == 0 || !eventStatus.getRequestModeration()) {
            status = ParticipationStatus.CONFIRMED;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);

        statsCollectorClient.sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, LocalDateTime.now());

        return requestMapper.toDto(savedRequest);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: request_id = " + requestId));

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException("Можно отменить только свою заявку");
        }

        request.setStatus(ParticipationStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        EventStatusDto eventStatus = eventClient.getEventStatus(eventId);
        if (!eventStatus.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором этого события");
        }

        return requestRepository.findByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestDto) {
        EventStatusDto eventStatus = eventClient.getEventStatus(eventId);
        if (!eventStatus.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором этого события");
        }

        List<Long> requestIds = requestDto.getRequestIds();
        if (requestIds == null || requestIds.isEmpty()) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

        boolean hasNotPending = requests.stream()
                .anyMatch(r -> r.getStatus() != ParticipationStatus.PENDING);
        if (hasNotPending) {
            throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
        }

        ParticipationStatus targetStatus = requestDto.getStatus();
        if (targetStatus != ParticipationStatus.CONFIRMED && targetStatus != ParticipationStatus.REJECTED) {
            throw new ConflictException("Доступные статусы только CONFIRMED/REJECTED");
        }

        if (targetStatus == ParticipationStatus.REJECTED) {
            requests.forEach(r -> r.setStatus(ParticipationStatus.REJECTED));
            List<ParticipationRequest> saved = requestRepository.saveAll(requests);

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(requestMapper.toDtoList(saved))
                    .build();
        }

        Integer limit = eventStatus.getParticipantLimit();
        if (limit != null && limit > 0) {
            long alreadyConfirmed = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
            long available = limit - alreadyConfirmed;

            if (available <= 0) {
                throw new ConflictException("Достигнут лимит по заявкам на данное событие");
            }

            List<ParticipationRequest> toConfirm = new ArrayList<>();
            List<ParticipationRequest> toReject = new ArrayList<>();

            for (ParticipationRequest r : requests) {
                if (available > 0) {
                    r.setStatus(ParticipationStatus.CONFIRMED);
                    toConfirm.add(r);
                    available--;
                } else {
                    r.setStatus(ParticipationStatus.REJECTED);
                    toReject.add(r);
                }
            }

            requestRepository.saveAll(requests);

            long totalConfirmed = alreadyConfirmed + toConfirm.size();
            if (totalConfirmed >= limit) {
                List<ParticipationRequest> pending = requestRepository.findAllByEventIdAndStatus(eventId, ParticipationStatus.PENDING);
                pending.forEach(r -> r.setStatus(ParticipationStatus.REJECTED));
                requestRepository.saveAll(pending);
            }

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(requestMapper.toDtoList(toConfirm))
                    .rejectedRequests(requestMapper.toDtoList(toReject))
                    .build();
        }

        requests.forEach(r -> r.setStatus(ParticipationStatus.CONFIRMED));
        List<ParticipationRequest> saved = requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toDtoList(saved))
                .rejectedRequests(List.of())
                .build();
    }
}