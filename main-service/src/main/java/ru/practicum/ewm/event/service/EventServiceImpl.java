package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.mapper.LocationMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.enums.EventSort;
import ru.practicum.ewm.event.model.enums.EventState;
import ru.practicum.ewm.event.model.enums.EventStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatsClient statClient;
    private final EntityManager entityManager;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Collection<EventDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> catIds,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable page) {
        List<EventState> stateList = null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (states != null) {
            stateList = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }
        if (rangeStart != null) {
            start = rangeStart;
        }
        if (rangeEnd != null) {
            end = rangeEnd;
        }
        List<Event> events = eventRepository.findEventsByAdmin(users, stateList, catIds, start, end, page);
        return events.stream()
                .map(EventMapper::toEventDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventDto updateEvent(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequest) {
        Event event = getEventById(eventId);
        if (updateEventAdminRequest == null) {
            return EventMapper.toEventDto(event);
        }

        if (updateEventAdminRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventAdminRequest.getAnnotation());
        }
        if (updateEventAdminRequest.getCategory() != null) {
            Category category = getCategoryById(updateEventAdminRequest.getCategory());
            event.setCategory(category);
        }
        if (updateEventAdminRequest.getDescription() != null) {
            event.setDescription(updateEventAdminRequest.getDescription());
        }
        if (updateEventAdminRequest.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(updateEventAdminRequest.getLocation()));
        }
        if (updateEventAdminRequest.getPaid() != null) {
            event.setPaid(updateEventAdminRequest.getPaid());
        }
        if (updateEventAdminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit().intValue());
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }
        if (updateEventAdminRequest.getTitle() != null) {
            event.setTitle(updateEventAdminRequest.getTitle());
        }
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction().equals(EventStateAction.PUBLISH_EVENT)) {
                if (event.getPublishedOn() != null) {
                    throw new ConflictException("Мероприятие уже опубликован.");
                }
                if (event.getState().equals(EventState.CANCELED)) {
                    throw new ConflictException("Мероприятие уже отменено.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (updateEventAdminRequest.getStateAction().equals(EventStateAction.REJECT_EVENT)) {
                if (event.getPublishedOn() != null) {
                    throw new ConflictException("Мероприятие уже опубликован.");
                }
                event.setState(EventState.CANCELED);
            }
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            LocalDateTime eventDateTime = updateEventAdminRequest.getEventDate();
            if (eventDateTime.isBefore(LocalDateTime.now())
                    || eventDateTime.isBefore(event.getPublishedOn().plusHours(2))) {
                throw new ValidationException("Мероприятие не может быть раньше, чем через 2 часа от текущего времени. ");
            }

            event.setEventDate(updateEventAdminRequest.getEventDate());
        }

        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEvent(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndPublishedOnIsNotNull(id)
                .orElseThrow(() -> new NotFoundException(String.format("Can't find event with id = %s event doesn't exist", id)));
        setView(event);
        sendStat(event, request);
        return EventMapper.toEventDto(event);
    }

    @Override
    public Collection<EventDto> getAllEvents(String text, List<Long> categories,
                                             Boolean paid, LocalDateTime rangeStart,
                                             LocalDateTime rangeEnd, Boolean onlyAvailable,
                                             EventSort sort, Integer from, Integer size, HttpServletRequest request) {
        LocalDateTime start = rangeStart != null ? rangeStart : null;
        LocalDateTime end = rangeEnd != null ? rangeEnd : null;

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = builder.createQuery(Event.class);

        Root<Event> root = query.from(Event.class);
        Predicate criteria = builder.conjunction();

        if (text != null) {
            Predicate annotationContain = builder.like(builder.lower(root.get("annotation")),
                    "%" + text.toLowerCase() + "%");
            Predicate descriptionContain = builder.like(builder.lower(root.get("description")),
                    "%" + text.toLowerCase() + "%");
            Predicate containText = builder.or(annotationContain, descriptionContain);

            criteria = builder.and(criteria, containText);
        }

        if (categories != null && categories.size() > 0) {
            Predicate containStates = root.get("category").in(categories);
            criteria = builder.and(criteria, containStates);
        }

        if (paid != null) {
            Predicate isPaid;
            if (paid) {
                isPaid = builder.isTrue(root.get("paid"));
            } else {
                isPaid = builder.isFalse(root.get("paid"));
            }
            criteria = builder.and(criteria, isPaid);
        }

        if (rangeStart != null) {
            Predicate greaterTime = builder.greaterThanOrEqualTo(root.get("eventDate").as(LocalDateTime.class), start);
            criteria = builder.and(criteria, greaterTime);
        }
        if (rangeEnd != null) {
            Predicate lessTime = builder.lessThanOrEqualTo(root.get("eventDate").as(LocalDateTime.class), end);
            criteria = builder.and(criteria, lessTime);
        }

        query.select(root).where(criteria).orderBy(builder.asc(root.get("eventDate")));
        List<Event> events = entityManager.createQuery(query)
                .setFirstResult(from)
                .setMaxResults(size)
                .getResultList();

        if (onlyAvailable) {
            events = events.stream()
                    .filter((event -> event.getConfirmedRequests() < (long) event.getParticipantLimit()))
                    .collect(Collectors.toList());
        }

        if (sort != null) {
            if (sort.equals(EventSort.EVENT_DATE)) {
                events = events.stream().sorted(Comparator.comparing(Event::getEventDate)).collect(Collectors.toList());
            } else {
                events = events.stream().sorted(Comparator.comparing(Event::getViews)).collect(Collectors.toList());
            }
        }

        if (events.size() == 0) {
            return new ArrayList<>();
        }

        setView(events);
        sendStat(events, request);
        return events.stream()
                .map(EventMapper::toEventDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDto> getEventUser(Long userId, Pageable page) {
        checkUserExist(userId);
        List<Event> events = eventRepository.getAllByInitiatorId(userId, page).toList();
        return events.stream()
                .map(EventMapper::toEventDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventDto addEventUser(Long userId, NewEventDto newEventDto) {
        Category category = getCategoryById(newEventDto.getCategory());
        LocalDateTime eventDate = newEventDto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Начало мероприятия не может быть раньше, чем через 2 часа от текущего времени. ");
        }
        User user = getUserById(userId);
        Event event = EventMapper.toEvent(newEventDto, category, user, eventDate);
        return EventMapper.toEventDto(eventRepository.save(event));
    }

    @Override
    public EventDto getFullEventUser(Long userId, Long eventId) {
        checkUserExist(userId);
        Event event = getEventById(eventId);
        return EventMapper.toEventDto(event);
    }

    @Override
    public EventDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event event = getEventById(eventId);

        if (event.getPublishedOn() != null) {
            throw new ConflictException("Мероприятие уже опубликован.");
        }

        if (updateEventUserRequest == null) {
            return EventMapper.toEventDto(event);
        }

        if (updateEventUserRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventUserRequest.getAnnotation());
        }
        if (updateEventUserRequest.getCategory() != null) {
            event.setCategory(getCategoryById(updateEventUserRequest.getCategory()));
        }
        if (updateEventUserRequest.getDescription() != null) {
            event.setDescription(updateEventUserRequest.getDescription());
        }
        if (updateEventUserRequest.getEventDate() != null) {
            LocalDateTime eventDateTime = updateEventUserRequest.getEventDate();
            if (eventDateTime.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Начало мероприятия не может быть раньше, чем через 2 часа от текущего времени. ");
            }
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(updateEventUserRequest.getLocation()));
        }
        if (updateEventUserRequest.getPaid() != null) {
            event.setPaid(updateEventUserRequest.getPaid());
        }
        if (updateEventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit().intValue());
        }
        if (updateEventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
        }

        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction().equals(EventStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        return EventMapper.toEventDto(eventRepository.save(event));
    }

    private Category getCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id={} не найдена.", catId)));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id={} не найден.", userId)));
    }

    private void checkUserExist(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id={} не найден.", userId));
        }
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id={} не найдено", eventId)));
    }

    public void sendStat(Event event, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String nameService = "main-service";
        statClient.create(
                EndpointHitDto.builder()
                        .timestamp(LocalDateTime.now())
                        .uri("/events")
                        .ip(remoteAddr)
                        .app(nameService)
                        .build());
        sendStatForTheEvent(event.getId(), remoteAddr, LocalDateTime.now(), nameService);
    }

    public void sendStat(List<Event> events, HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String remoteAddr = request.getRemoteAddr();
        String nameService = "main-service";

        statClient.create(
                EndpointHitDto.builder()
                        .timestamp(LocalDateTime.now())
                        .uri("/events")
                        .ip(request.getRemoteAddr())
                        .app(nameService)
                        .build());

        sendStatForEveryEvent(events, remoteAddr, LocalDateTime.now(), nameService);
    }

    private void sendStatForTheEvent(Long eventId, String remoteAddr, LocalDateTime now,
                                     String nameService) {
        statClient.create(
                EndpointHitDto.builder()
                        .timestamp(LocalDateTime.now())
                        .uri("/events/" + eventId)
                        .ip(remoteAddr)
                        .app(nameService)
                        .build());
    }

    private void sendStatForEveryEvent(List<Event> events, String remoteAddr, LocalDateTime now,
                                       String nameService) {
        for (Event event : events) {
            statClient.create(EndpointHitDto.builder()
                    .timestamp(LocalDateTime.now())
                    .uri("/events/" + event.getId())
                    .app(nameService)
                    .ip(remoteAddr)
                    .build());
        }
    }

    public void setView(List<Event> events) {
        LocalDateTime start = events.get(0).getCreatedOn();
        List<String> uris = new ArrayList<>();
        Map<String, Event> eventsUri = new HashMap<>();
        String uri = "";
        for (Event event : events) {
            if (start.isBefore(event.getCreatedOn())) {
                start = event.getCreatedOn();
            }
            uri = "/events/" + event.getId();
            uris.add(uri);
            eventsUri.put(uri, event);
            event.setViews(0L);
        }

        String startTime = start.format(dateFormatter);
        String endTime = LocalDateTime.now().format(dateFormatter);

        List<ViewStatsDto> stats = getStats(startTime, endTime, uris);
        stats.forEach((stat) ->
                eventsUri.get(stat.getUri()).setViews(stat.getHits()));
    }

    public void setView(Event event) {
        String startTime = event.getCreatedOn().format(dateFormatter);
        String endTime = LocalDateTime.now().format(dateFormatter);
        List<String> uris = List.of("/events/" + event.getId());

        List<ViewStatsDto> stats = getStats(startTime, endTime, uris);
        if (stats.size() == 1) {
            event.setViews(stats.get(0).getHits());
        } else {
            event.setViews(0L);
        }
    }

    private List<ViewStatsDto> getStats(String startTime, String endTime, List<String> uris) {
        return statClient.getStats(startTime, endTime, uris, false);
    }
}

