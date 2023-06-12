package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequestDto;
import ru.practicum.ewm.event.service.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping("{userId}/events")
    @ResponseStatus(HttpStatus.OK)
    public List<EventDto> getEventsUser(@PathVariable @Positive Long userId,
                                        @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                        @RequestParam(defaultValue = "10") @Positive int size) {
        PageRequest page = PageRequest.of(from, size);
        return eventService.getEventUser(userId, page);
    }

    @PostMapping("{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto addEventUser(@PathVariable @Positive Long userId,
                                 @RequestBody @Valid NewEventDto newEventDto) {
        return eventService.addEventUser(userId, newEventDto);
    }

    @GetMapping("{userId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto getFullEventUser(@PathVariable @Positive Long userId,
                                     @PathVariable @Positive Long eventId) {
        return eventService.getFullEventUser(userId, eventId);
    }

    @PatchMapping("{userId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto updateEventUser(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequestDto updateEventUserRequest) {
        return eventService.updateEventUser(userId, eventId, updateEventUserRequest);
    }
}

