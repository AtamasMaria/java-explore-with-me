package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdate;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.practicum.ewm.event.model.enums.EventState.PUBLISHED;
import static ru.practicum.ewm.request.model.RequestStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public EventRequestStatusUpdateResult changeStatusRequest(Long userId, Long eventId, EventRequestStatusUpdate updateDto) {
        if (updateDto.getStatus() == null || updateDto.getRequestIds() == null) {
            throw new ConflictException("Некорректный запрос, отсутствует статус или идентификаторы для замены.");
        }
        checkUserExists(userId);
        Event event = getEventById(eventId);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Подтверждение заявок не требуется.");
        }
        List<Long> requestsId = updateDto.getRequestIds();
        for (Long requestId : requestsId) {
            Request request = getRequestById(requestId);
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус заявки остается неизменным.");
            }
            if (updateDto.getStatus() == CONFIRMED) {
                if (event.getParticipantLimit() <= getConfirmedRequestsCount(event.getRequests())) {
                    throw new ConflictException("Превышен лимит участников мероприятия.");
                } else {
                    request.setStatus(CONFIRMED);
                }
            } else if (updateDto.getStatus() == REJECTED) {
                request.setStatus(REJECTED);
            }
        }
        List<ParticipationRequestDto> confirmedRequests = requestRepository.findAllByIdInAndStatus(requestsId,
                        CONFIRMED).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejectedRequests = requestRepository.findAllByIdInAndStatus(requestsId,
                        REJECTED).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ParticipationRequestDto addUserRequest(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventById(eventId);
        Optional<Request> request = requestRepository.findByRequesterIdAndEventId(userId, eventId);
        if (request.isPresent()) {
            throw new ConflictException("Запрос на участие уже существует.");
        }
        if (userId.equals(event.getInitiator().getId())) {
            throw new ConflictException("Инициатор мероприятия не может добавить запрос на свое событие.");
        }
        if (event.getState() != PUBLISHED) {
            throw new ConflictException("Событие еще не было опубликовано.");
        }

        if (event.getParticipantLimit() <= getConfirmedRequestsCount(event.getRequests())) {
            throw new ConflictException("The limit of participants has expired");

        } else if (event.getRequestModeration() != null && !event.getRequestModeration()) {
            return RequestMapper.toParticipationRequestDto(requestRepository.save(
                    Request.builder()
                            .created(LocalDateTime.now())
                            .requesterId(userId)
                            .event(event)
                            .status(CONFIRMED)
                            .build()));
        } else {
            return RequestMapper.toParticipationRequestDto(requestRepository.save(
                    Request.builder()
                            .created(LocalDateTime.now())
                            .requesterId(userId)
                            .event(event)
                            .status(PENDING)
                            .build()));
        }
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancelRequestOwner(Long userId, Long requestId) {
        checkUserExists(userId);
        Request request = getRequestById(requestId);
        if (request.getStatus() == REJECTED || request.getStatus() == CANCELED) {
            throw new ConflictException("Запрос уже был отклонен.");
        }
        request.setStatus(CANCELED);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsUser(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Информация по этому мероприятию доступна только инициатору.");
        }
        return event.getRequests().stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    public int getConfirmedRequestsCount(List<Request> requests) {
        if (requests == null) {
            return 0;
        }
        return (int) requests.stream().filter(r -> r.getStatus() == CONFIRMED).count();
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

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id={} не найден.", userId));
        }
    }
}