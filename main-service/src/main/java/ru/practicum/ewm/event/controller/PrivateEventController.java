package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;

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
        PageRequest page = PageRequest.of(from, size);
        return eventService.getEventUser(userId, page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto addEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                 @RequestBody @Valid NewEventDto newEventDto) {
        return eventService.addEventUser(userId, newEventDto);
    }

    @GetMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto getFullEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                     @PathVariable(name = "eventId") @Positive Long eventId) {
        return eventService.getFullEventUser(userId, eventId);
    }

    @PatchMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto updateEventUser(@PathVariable(name = "userId") @Positive Long userId,
                                    @PathVariable(name = "eventId") @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequestDto updateEventUserRequest) {
        return eventService.updateEventUser(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventsRequests(@PathVariable(name = "userId") @Positive Long userId,
                                                           @PathVariable(name = "eventId") @Positive Long eventId) {
        return requestService.getRequestsUser(userId, eventId);
    }

    @PatchMapping("{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult changeStatusRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                              @PathVariable(name = "eventId") @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdate statusUpdate) {
        return requestService.changeStatusRequest(userId, eventId, statusUpdate);
    }
}

