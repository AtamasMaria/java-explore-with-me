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
    public Collection<EventDto> getEventsByAdmin(List<Long> users, List<String> states,
                                                 List<Long> categories, LocalDateTime rangeStart,
                                                 LocalDateTime rangeEnd, Pageable page) {
        List<EventState> states1 = null;
        if (states != null) {
            states1 = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? rangeStart : null;
        LocalDateTime end = rangeEnd != null ? rangeEnd : null;

        List<Event> events = eventRepository.getEventsWithUsersStatesCategoriesDateTime(
                users, states1, categories, start, end, page);

        Map<Long, Long> views = statisticService.getStatsEvents(events);
        return events.stream()
                .map(EventMapper::toEventDto)
                .peek(e -> e.setViews(views.get(e.getId())))
                .collect(Collectors.toList());
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
        statisticService.addView(request);
        Map<Long, Long> views = statisticService.getStatsEvents(List.of(event));
        EventDto eventDto = EventMapper.toEventDto(event);
        eventDto.setViews(views.getOrDefault(event.getId(), 0L));
        return eventDto;
    }

    @Override
    public Collection<EventShortDto> getAllEvents(String text, List<Long> categories, Boolean paid,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             boolean onlyAvailable, String sort, Pageable page, HttpServletRequest request) {
        checkStartAndEnd(rangeStart, rangeEnd);

        List<Event> events = new ArrayList<>();

        if (onlyAvailable) {
            if (sort == null) {
                events = eventRepository.getAvailableEventsWithFiltersDateSorted(
                        text, EventState.PUBLISHED, categories, paid, start, end, page);
            } else {
                switch (EventSort.valueOf(sort)) {
                    case EVENT_DATE:
                        events = eventRepository.getAvailableEventsWithFiltersDateSorted(
                                text, EventState.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        statisticService.addView(request);
                        Map<Long, Long> view = statisticService.getStatsEvents(events);
                        return events.stream()
                                .map(EventMapper::toEventShortDto)
                                .peek(e -> e.setViews(view.get(e.getId())))
                                .collect(Collectors.toList());
                    case VIEWS:
                        events = eventRepository.getAvailableEventsWithFilters(
                                text, EventState.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        statisticService.addView(request);
                        Map<Long, Long> view1 = statisticService.getStatsEvents(events);
                        return events.stream()
                                .map(EventMapper::toEventShortDto)
                                .peek(e -> e.setViews(view1.get(e.getId())))
                                .sorted(Comparator.comparing(EventShortDto::getViews))
                                .collect(Collectors.toList());
                }
            }
        } else {
            if (sort == null) {
                events = eventRepository.getAllEventsWithFiltersDateSorted(
                        text, EventState.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
            } else {
                switch (EventSort.valueOf(sort)) {
                    case EVENT_DATE:
                        events = eventRepository.getAllEventsWithFiltersDateSorted(
                                text, EventState.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        statisticService.addView(request);
                        Map<Long, Long> view = statisticService.getStatsEvents(events);
                        return events.stream()
                                .map(EventMapper::toEventShortDto)
                                .peek(e -> e.setViews(view.get(e.getId())))
                                .collect(Collectors.toList());
                    case VIEWS:
                        events = eventRepository.getAllEventsWithFilters(
                                text, EventState.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        statisticService.addView(request);
                        Map<Long, Long> view1 = statisticService.getStatsEvents(events);
                        return events.stream()
                                .map(EventMapper::toEventShortDto)
                                .peek(e -> e.setViews(view1.get(e.getId())))
                                .sorted(Comparator.comparing(EventShortDto::getViews))
                                .collect(Collectors.toList());
                }
            }
        }
        statisticService.addView(request);
        Map<Long, Long> view = statisticService.getStatsEvents(events);
        return events.stream()
                .map(EventMapper::toEventShortDto)
                .peek(e -> e.setViews(view.get(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getEventUser(Long userId, Pageable page) {
        checkUserExists(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);
        Map<Long, Long> views = statisticService.getStatsEvents(events);
        return events.stream()
                .map(EventMapper::toEventShortDto)
                .peek(e -> e.setViews(views.getOrDefault(e.getId(), 0L)))
                .collect(toList());
    }

    @Override
    @Transactional
    public EventDto addEventUser(Long userId, NewEventDto newEventDto) {
        checkValidEventDate(newEventDto.getEventDate());
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());
        Event event = EventMapper.toEvent(newEventDto, category, user);
        eventRepository.save(event);

        Map<Long, Long> views = statisticService.getStatsEvents(List.of(event));
        EventDto eventDto = EventMapper.toEventDto(event);
        eventDto.setViews(views.getOrDefault(event.getId(), 0L));

        return eventDto;
    }

    @Override
    public EventDto getFullEventUser(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventById(eventId);
        Map<Long, Long> views = statisticService.getStatsEvents(List.of(event));
        EventDto eventDto = EventMapper.toEventDto(event);
        eventDto.setViews(views.getOrDefault(event.getId(), 0L));
        return eventDto;
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
        Map<Long, Long> views = statisticService.getStatsEvents(List.of(event));
        EventDto eventDto = EventMapper.toEventDto(eventRepository.save(event));
        eventDto.setViews(views.getOrDefault(event.getId(), 0L));

        return eventDto;
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

        Event event = getEventById(eventId);
        List<Request> requestsEvent = requestRepository.findAllByEventId(eventId);

        if (event.getParticipantLimit() <= event.getConfirmedRequests()) {
            throw new ConflictException("Превышен лимит участников мероприятия.");
        }

        EventRequestStatusUpdateResult eventRequestStatusUpdateResult = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(new ArrayList<>())
                .rejectedRequests(new ArrayList<>())
                .build();

        List<Request> requestListUpdateStatus = new ArrayList<>();
        for (Request request : requestsEvent) {
            if (updateDto.getRequestIds().contains(request.getId())) {
                request.setStatus(updateDto.getStatus());
                requestListUpdateStatus.add(request);
            }
        }

        for (Request request : requestListUpdateStatus) {
            if (!request.getStatus().equals(RequestStatus.CANCELED)) {
                eventResultConstructor(eventRequestStatusUpdateResult, request, event);
            } else {
                throw new ConflictException("Ошибка.");
            }
        }

        requestRepository.saveAll(requestListUpdateStatus);
        return eventRequestStatusUpdateResult;
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
                event.setPublishedOn(LocalDateTime.now());
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
            throw new NotFoundException("Событие еще не опубликованно.");
        }
    }

    private void checkEventsStatePublishedOrCanceled(Event event) {
        if (event.getState().equals(EventState.PUBLISHED) || event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Нельзя редактировать опубликованное или отклонненое событие.");
        }
    }

    private void eventResultConstructor(
            EventRequestStatusUpdateResult eventRequestStatusUpdateResult, Request request, Event event) {
        if (request.getStatus().equals(RequestStatus.CONFIRMED)) {
            eventRequestStatusUpdateResult.getConfirmedRequests().add(RequestMapper.toParticipationRequestDto(request));
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        } else if (request.getStatus().equals(RequestStatus.REJECTED)) {
            eventRequestStatusUpdateResult.getRejectedRequests().add(RequestMapper.toParticipationRequestDto(request));
        }
    }

    private void checkStartAndEnd(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            start = LocalDateTime.now().minusYears(10);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        if (start.isAfter(end)) {
            throw new ValidationException("Окончание события не может быть раньше начала события.");
        }
    }
}