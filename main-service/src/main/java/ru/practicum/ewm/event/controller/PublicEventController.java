package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventShortDto> getAllEvents(@RequestParam(required = false) String text,
                                                  @RequestParam(required = false) List<Long> categories,
                                                  @RequestParam(required = false) Boolean paid,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                             LocalDateTime rangeStart,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                             LocalDateTime rangeEnd,
                                                  @RequestParam(required = false) boolean onlyAvailable,
                                                  @RequestParam(required = false) String sort,
                                                  @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                                  @RequestParam(required = false, defaultValue = "10") @Positive Integer size,
                                                  HttpServletRequest httpRequest) {
        log.info("GET-Получение событий с фильтрацией.");
        PageRequest page = PageRequest.of(from, size);
        return eventService.getAllEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort,
                page, httpRequest);
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto getFullEvent(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.info("GET-Получение подробной информации об опубликованном событии по идентификатору.");
        return eventService.getFullEvent(id, httpRequest);
    }
}
