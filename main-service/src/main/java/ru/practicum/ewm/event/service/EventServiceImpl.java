package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.mapper.LocationMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.model.enums.EventSort;
import ru.practicum.ewm.event.model.enums.EventState;
import ru.practicum.ewm.event.model.enums.EventStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RequestsStatusException;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.statistic.StatisticService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


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
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (rangeStart != null) {
            start = rangeStart;
        }
        if (rangeEnd != null) {
            end = rangeEnd;
        }
        if (states != null) {
            states1 = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findEventsByAdmin(users, categories, states1, rangeStart, rangeEnd, page);
        for (Event event : events) {
            statisticService.setEventViews(event);
            setConfirmedRequests(event);
        }
        return events
                .stream()
                .map(EventMapper::toEventDto)
                .collect(toList());
    }

    @Override
    public EventDto updateEvent(Long eventId, UpdateEventAdminRequestDto updateEventDto) {
        Event event = getEventById(eventId);
        checkValidNewEvenDateByAdmin(updateEventDto);
        checkParticipationStatusIsPending(event.getState());

        if (updateEventDto.getEventDate() != null) {
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
            event.setLocation(LocationMapper.toLocation(updateEventDto.getLocation()));
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
        fillEventState(event, updateEventDto.getStateAction());
        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEvent(Long id, HttpServletRequest request) {
        Event event = getEventById(id);
        checkEventStatePublished(event);
        setConfirmedRequests(event);
        statisticService.addHit(request);
        statisticService.setEventViews(event);
        return EventMapper.toEventDto(event);
    }

    @Override
    public Collection<EventDto> getAllEvents(String text, List<Long> categories, Boolean paid,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Boolean onlyAvailable, EventSort sort, Pageable page, HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null) {
            if (rangeStart.isAfter(rangeEnd)) {
                throw new ValidationException("Дата окончания события задана позже даты старта");
            }
        }
        List<Event> events = eventRepository.findEventsByUser(categories, paid, EventState.PUBLISHED, rangeStart, rangeEnd,
                text, page);
        statisticService.addHit(request);
        Set<Long> eventsIds = getEventIds(events);


        if (onlyAvailable.equals(Boolean.TRUE)) {
            events = selectOnlyAvailableEvents(events, eventsIds);
        }

        if (sort != null) {
            return getSortedEventsShortDto(events, sort);
        }

        return events.stream()
                .peek(statisticService::setEventViews)
                .map(EventMapper::toEventDto)
                .collect(toList());
    }

    @Override
    public List<EventDto> getEventUser(Long userId, Pageable page) {
        checkUserExists(userId);
        return eventRepository.findAllByInitiatorId(page, userId)
                .stream()
                .peek(statisticService::setEventViews)
                .map(EventMapper::toEventDto)
                .collect(toList());
    }

    @Override
    public EventDto addEventUser(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Время мероприятия не может быть раньше текущего.");
        }
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        Event event = EventMapper.toEvent(newEventDto, category, user);
        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEventUser(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventById(eventId);
        statisticService.setEventViews(event);
        return EventMapper.toEventDto(event);
    }

    @Override
    public EventDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventDto) {
        if (updateEventDto.getEventDate().plusHours(2L).isAfter(LocalDateTime.now())) {
            throw new ValidationException("Мероприятие не может быть раньше, чем через 2 часа от текущего времени.");
        }
        Event event = getEventById(eventId);

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
       fillEventState(event, updateEventDto.getStateAction());

        return EventMapper.toEventDto(eventRepository.save(event));
    }

    private void setConfirmedRequests(Event event) {
        List<Request> requestList = requestRepository.findByIdInAndStatus(event.getId(), RequestStatus.CONFIRMED);
        event.setConfirmedRequests((long) requestList.size());
    }

    private List<EventDto> getSortedEventsShortDto(List<Event> events, EventSort sort) {
        switch (sort) {
            case VIEWS:
                return events.stream()
                        .map(EventMapper::toEventDto)
                        .sorted(Comparator.comparing(EventDto::getViews).reversed())
                        .collect(toList());
            case EVENT_DATE:
                return events.stream()
                        .map(EventMapper::toEventDto)
                        .sorted(Comparator.comparing(EventDto::getEventDate).reversed())
                        .collect(toList());
            default:
                throw new ValidationException(String
                        .format("Неизвестная команда."));
        }
    }

    private Set<Long> getEventIds(List<Event> events) {
        Set<Long> eventIds = new HashSet<>();

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

    private List<Event> selectOnlyAvailableEvents(List<Event> events, Set<Long> eventIds) {
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
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case CANCEL_REVIEW:
                event.setState(EventState.CANCELED);
                break;
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
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

    private void checkValidNewEvenDateByAdmin(UpdateEventAdminRequestDto eventUpdateDto) {
        LocalDateTime eventNewDate = eventUpdateDto.getEventDate();
        if (eventNewDate == null) {
            return;
        }
        if (eventNewDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Мероприятие не может быть раньше, чем через 2 часа от текущего времени. ");
        }
    }

    private void checkParticipationStatusIsPending(EventState state) {
        if (state.equals(RequestStatus.PENDING)) {
            throw new RequestsStatusException("Запрос должен иметь статус PENDING");
        }
    }

    private static void checkEventStatePublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException (String
                    .format("Событие еще не опубликованно."));
        }
    }
}