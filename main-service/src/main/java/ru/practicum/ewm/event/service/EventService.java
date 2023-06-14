package ru.practicum.ewm.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.event.dto.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventService {
    Collection<EventDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable page);

    EventDto updateEvent(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequest);

    EventDto getFullEvent(Long id, HttpServletRequest request);

    Collection<EventDto> getAllEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, Boolean onlyAvailable, String sort,
                                      Pageable page, HttpServletRequest request);

    List<EventShortDto> getEventUser(Long userId, Pageable page);

    EventDto addEventUser(Long userId, NewEventDto newEventDto);

    EventDto getFullEventUser(Long userId, Long eventId);

    EventDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventUserRequest);
}
