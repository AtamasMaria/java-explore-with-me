package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.enums.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public ParticipationRequestDto addUserRequest(Long userId, Long eventId) {
        User requester = getUserById(userId);
        Event event = getEventById(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос на участие уже существует.");
        }
        if (userId.equals(event.getInitiator().getId())) {
            throw new ConflictException("Инициатор мероприятия не может добавить запрос на свое событие.");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие еще не было опубликовано.");
        }
        if (event.getParticipantLimit() != 0) {
            if (event.getParticipantLimit() <= requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)) {
                throw new ConflictException("Лимит участников достигнут. Больше нет возможности добавить запрос.");
            }
        }
        Request request = new Request();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request = Request.builder()
                    .created(LocalDateTime.now())
                    .requester(requester)
                    .event(event)
                    .status(RequestStatus.CONFIRMED)
                    .build();
            eventRepository.save(event);
        } else {
            request = Request.builder()
                    .created(LocalDateTime.now())
                    .requester(requester)
                    .event(event)
                    .status(RequestStatus.PENDING)
                    .build();
        }
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancelRequestOwner(Long userId, Long requestId) {
        getUserById(userId);
        Request request = getRequestById(requestId);
        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsUser(Long userId) {
        getUserById(userId);
        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Запрос на участие с id={} не найден.", requestId)));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id={} не найден.", userId)));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Мероприятие с id={} не найдено.", eventId)));
    }
}