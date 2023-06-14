package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequestDto;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdate;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class PrivateEventController {
    private final EventService eventService;
    private final RequestService requestService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsUser(@PathVariable @Positive Long userId,
                                             @RequestParam(required = false, defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(required = false, defaultValue = "10") @Positive int size) {
        log.info("GET-Получение событий, добавленных текущим пользователем.");
        PageRequest page = PageRequest.of(from, size);
        return eventService.getEventUser(userId, page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto addEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                 @RequestBody @Valid NewEventDto newEventDto) {
        log.info("POST-Добавление нового события.");
        return eventService.addEventUser(userId, newEventDto);
    }

    @GetMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto getFullEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                     @PathVariable(name = "eventId") @Positive Long eventId) {
        log.info("GET-Получение полной информации о событии добывленном текуцим пользователем.");
        return eventService.getFullEventUser(userId, eventId);
    }

    @PatchMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto updateEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                    @PathVariable(name = "eventId") @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequestDto updateEventUserRequest) {
        log.info("PATCH-Изменение события добавленного текущим пользователем.");
        return eventService.updateEventUser(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getAllRequestsByEventId(@PathVariable(name = "userId") @Positive Long userId,
                                                           @PathVariable(name = "eventId") @Positive Long eventId) {
        log.info("GET-Получение информации о запросах на участие в событии текущего пользователя.");
        return requestService.getRequestsUser(userId, eventId);
    }

    @PatchMapping("{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult changeStatusRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                              @PathVariable(name = "eventId") @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdate statusUpdate) {
        log.info("PATCH-Изменение статуса(подтверждена/отменена) заявок на участие в событии текущего пользователя");
        return requestService.changeStatusRequest(userId, eventId, statusUpdate);
    }
}

