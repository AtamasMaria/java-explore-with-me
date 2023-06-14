package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.service.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {

    private final EventService eventService;

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventDto> getEvents(@RequestParam(required = false) List<Long> users,
                                          @RequestParam(required = false) List<String> states,
                                          @RequestParam(required = false) List<Long> categories,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                          @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                          @RequestParam(defaultValue = "10") @Positive Integer size) {
        PageRequest page = PageRequest.of(from, size);
        return eventService.getEventsByAdmin(users, states, categories, rangeStart, rangeEnd, page);
    }

    @PatchMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto updateEvent(@PathVariable(name = "eventId") @Positive Long eventId,
                                @RequestBody @Valid UpdateEventAdminRequestDto updateEventAdminRequest) {
        return eventService.updateEvent(eventId, updateEventAdminRequest);
    }
}
