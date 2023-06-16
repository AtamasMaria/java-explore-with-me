package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.model.enums.EventSort;
import ru.practicum.ewm.event.model.enums.EventState;
import ru.practicum.ewm.event.model.enums.EventStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdate;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.statistic.StatisticService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static ru.practicum.ewm.request.service.RequestServiceImpl.getConfirmedRequestsCount;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatisticService statisticService;

    @Override
    public Collection<EventDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable page) {
        List<EventState> states1 = null;
        LocalDateTime start = rangeStart != null ? rangeStart : null;
        LocalDateTime end = rangeEnd != null ? rangeEnd : null;
        if (states != null) {
            states1 = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }
        List<Event> events = eventRepository.findAllByAdmin(users, categories, states1, start, end, page);
        for (Event event : events) {
            setConfirmedRequests(event);
        }
        return events
                .stream()
                .map(EventMapper::toEventDto)
                .collect(toList());
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long eventId, UpdateEventAdminRequestDto updateEventDto) {
        Event event = getEventById(eventId);
        checkParticipationStatusIsPending(event.getState());
        checkEventsStatePublishedOrCanceled(event);

        if (updateEventDto.getEventDate() != null) {
            checkValidEvenDateByAdmin(updateEventDto.getEventDate());
            event.setEventDate(updateEventDto.getEventDate());
        }
        if (updateEventDto.getTitle() != null && !(updateEventDto.getTitle().isBlank())) {
            event.setTitle(updateEventDto.getTitle());
        }
        if (updateEventDto.getAnnotation() != null && !(updateEventDto.getAnnotation().isBlank())) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null && !(updateEventDto.getDescription().isBlank())) {
            event.setDescription(updateEventDto.getDescription());
        }
        if (updateEventDto.getLocation() != null) {
            Location location = event.getLocation();
            location.setLon(updateEventDto.getLocation().getLon());
            location.setLat(updateEventDto.getLocation().getLat());
        }
        if (updateEventDto.getPaid() != null) {
            event.setPaid(updateEventDto.getPaid());
        }
        if (updateEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventDto.getParticipantLimit());
        }
        if (updateEventDto.getRequestModeration() != null) {
            event.setRequestModeration(updateEventDto.getRequestModeration());
        }
        if (updateEventDto.getCategory() != null) {
            Category category = getCategoryById(updateEventDto.getCategory());
            event.setCategory(category);
        }
        if (updateEventDto.getStateAction() != null) {
            fillEventState(event, updateEventDto.getStateAction());
        }

        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEvent(Long id, HttpServletRequest request) {
        Event event = getEventById(id);
        checkEventStatePublished(event);
        setConfirmedRequests(event);
        statisticService.addView(request);
        Map<Long, Long> hits = statisticService.getStatsEvents(List.of(event));
        event.setViews(hits.get(event.getId()));
        return EventMapper.toEventDto(event);
    }

    @Override
    public Collection<EventDto> getAllEvents(String text, List<Long> categories, Boolean paid,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Boolean onlyAvailable, String sort, Pageable page, HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null) {
            if (rangeStart.isAfter(rangeEnd) || rangeEnd.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Дата окончания события задана позже даты старта, " + "а так же дата окончания не может быть до настоящего времени.");
            }
        }
        Boolean paidParam = false;
        Boolean onlyAvailableParam = false;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusYears(1);

        if (paid != null) {
            paidParam = paid;
        }
        if (onlyAvailable != null) {
            onlyAvailableParam = onlyAvailable;
        }
        if (rangeStart != null) {
            start = rangeStart;
        }
        if (rangeEnd != null) {
            end = rangeEnd;
        }

        List<Event> events = eventRepository.findAllByUser(categories, paidParam, EventState.PUBLISHED, start, end, text, page);
        statisticService.addView(request);
        List<Long> eventsIds = getEventIds(events);

        if (onlyAvailableParam == true) {
            events = selectOnlyAvailableEvents(events, eventsIds);
        }
        return getSortedEventsShortDto(events, sort);
    }

    @Override
    public List<EventShortDto> getEventUser(Long userId, Pageable page) {
        checkUserExists(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);
        Map<Long, Long> views = statisticService.getStatsEvents(events);
        return events.stream().map(EventMapper::toEventShortDto).peek(e -> e.setViews(views.getOrDefault(e.getId(), 0L))).collect(toList());
    }

    @Override
    @Transactional
    public EventDto addEventUser(Long userId, NewEventDto newEventDto) {
        checkValidEventDate(newEventDto.getEventDate());
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        Event event = EventMapper.toEvent(newEventDto, category, user);
        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEventUser(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventById(eventId);
        Map<Long, Long> hits = statisticService.getStatsEvents(List.of(event));
        event.setViews(hits.getOrDefault(event.getId(), 0L));
        return EventMapper.toEventDto(event);
    }

    @Override
    @Transactional
    public EventDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventDto) {
        if (updateEventDto.getEventDate() != null) {
            checkValidEventDate(updateEventDto.getEventDate());
        }

        Event event = getEventById(eventId);
        checkUserExists(userId);

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Мероприятие уже опубликованно.");
        }

        if (updateEventDto.getEventDate() != null) {
            event.setEventDate(updateEventDto.getEventDate());
        }
        if (updateEventDto.getAnnotation() != null && !(updateEventDto.getAnnotation().isBlank())) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null && !(updateEventDto.getDescription().isBlank())) {
            event.setDescription(updateEventDto.getDescription());
        }
        if (updateEventDto.getLocation() != null) {
            Location location = event.getLocation();
            location.setLon(updateEventDto.getLocation().getLon());
            location.setLat(updateEventDto.getLocation().getLat());
        }
        if (updateEventDto.getPaid() != null) {
            event.setPaid(updateEventDto.getPaid());
        }
        if (updateEventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventDto.getParticipantLimit());
        }
        if (updateEventDto.getRequestModeration() != null) {
            event.setRequestModeration(updateEventDto.getRequestModeration());
        }
        if (updateEventDto.getTitle() != null && !(updateEventDto.getTitle().isBlank())) {
            event.setTitle(updateEventDto.getTitle());
        }
        if (updateEventDto.getCategory() != null) {
            Category category = getCategoryById(updateEventDto.getCategory());
            event.setCategory(category);
        }
        if (updateEventDto.getStateAction() != null) {
            fillEventState(event, updateEventDto.getStateAction());
        }
        Map<Long, Long> hits = statisticService.getStatsEvents(List.of(event));
        event.setViews(hits.getOrDefault(event.getId(), 0L));
        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId, Long eventId) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeStatusRequest(Long userId, Long eventId, EventRequestStatusUpdate updateDto) {
        getUserById(userId);
        checkRequestExist(userId, eventId);
        Event event = getEventById(eventId);

        List<Long> requestsId = updateDto.getRequestIds();
        for (Long requestId : requestsId) {
            Request request = getRequestById(requestId);
            if (updateDto.getStatus().equals(RequestStatus.CONFIRMED)) {
                if (event.getParticipantLimit() != 0 &&
                        event.getParticipantLimit() <= getConfirmedRequestsCount(requestRepository.findAllByEventId(eventId))) {
                    throw new ConflictException("Превышен лимит участников мероприятия.");
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                }
            } else if (updateDto.getStatus().equals(RequestStatus.REJECTED)) {
                request.setStatus(RequestStatus.REJECTED);
            }
        }
        List<ParticipationRequestDto> confirmedRequests = requestRepository.findAllByIdInAndStatus(requestsId,
                        RequestStatus.CONFIRMED).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejectedRequests = requestRepository.findAllByIdInAndStatus(requestsId,
                        RequestStatus.REJECTED).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    private void setConfirmedRequests(Event event) {
        List<Request> requestList = requestRepository.findAllByIdInAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);
        event.setConfirmedRequests((long) requestList.size());
    }

    private List<EventDto> getSortedEventsShortDto(List<Event> events, String sort) {
        Map<Long, Long> viewsMap = statisticService.getStatsEvents(events);
        if (sort.equals(EventSort.VIEWS.toString())) {
            return events.stream()
                    .map(EventMapper::toEventDto)
                    .peek(e -> e.setViews(viewsMap.get(e.getId())))
                    .sorted(Comparator.comparing(EventDto::getViews).reversed())
                    .collect(toList());
        }

        return events.stream()
                .map(EventMapper::toEventDto)
                .peek(e -> e.setViews(viewsMap.get(e.getId())))
                .sorted(Comparator.comparing(EventDto::getEventDate).reversed())
                .collect(toList());
    }

    private List<Long> getEventIds(List<Event> events) {
        List<Long> eventIds = new ArrayList<>();

        for (Event event : events) {
            eventIds.add(event.getId());
        }
        return eventIds;
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id={} не найдено", eventId)));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id={} не найден", userId)));
    }

    private Category getCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id={} не найдена", catId)));
    }

    private List<Event> selectOnlyAvailableEvents(List<Event> events, List<Long> eventIds) {
        List<Event> onlyAvailableEvents = new ArrayList<>();
        List<Request> confirmedRequest = requestRepository.findAllByIdInAndStatus(eventIds, RequestStatus.CONFIRMED);

        for (Event event : events) {
            Long participantLimit = event.getParticipantLimit();

            if (confirmedRequest.size() < participantLimit) {
                onlyAvailableEvents.add(event);
            }
        }
        return onlyAvailableEvents;
    }

    private void fillEventState(Event event, EventStateAction stateAction) {
        switch (stateAction) {
            case CANCEL_REVIEW:
                event.setState(EventState.CANCELED);
                break;
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
                break;
            case PUBLISH_EVENT:
                event.setState(EventState.PUBLISHED);
                break;
            case REJECT_EVENT:
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new ConflictException(String.format(String.format("ожидается состояние CANCEL_REVIEW or SEND_TO_REVIEW")));
        }
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id={} не найден", userId));
        }
    }

    private void checkValidEvenDateByAdmin(LocalDateTime eventDate) {
        if (LocalDateTime.now().plusHours(1).isAfter(eventDate)) {
            throw new ValidationException("Мероприятие не может быть раньше, чем через час до настоящего времени.");
        }
    }

    private void checkValidEventDate(LocalDateTime eventDate) {
        if (LocalDateTime.now().plusHours(2).isAfter(eventDate)) {
            throw new ValidationException("Мероприятие не может быть раньше, чем через 2 часа до настоящего времени.");
        }
    }

    private void checkParticipationStatusIsPending(EventState state) {
        if (!state.equals(EventState.PENDING)) {
            throw new ConflictException("Запрос должен иметь статус PENDING");
        }
    }

    private static void checkEventStatePublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException(String.format("Событие еще не опубликованно."));
        }
    }

    private void checkEventsStatePublishedOrCanceled(Event event) {
        if (event.getState().equals(EventState.PUBLISHED) || event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Нельзя редактировать опубликованное или отклонненое событие.");
        }
    }

    private void checkRequestExist(Long userId, Long eventId) {
        if (!requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new NotFoundException("Запрос не найден.");
        }
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Запрос на участие с id={} не найден.", requestId)));
    }
}