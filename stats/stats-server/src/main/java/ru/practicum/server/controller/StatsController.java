package ru.practicum.server.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.service.StatsService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController()
@AllArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService service;

    @PostMapping("/hit")
    public void create(@RequestBody @Valid EndpointHitDto endpointHitDto) {
        log.debug("Saving hit {}", endpointHitDto.getApp());
        service.create(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> get(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                  @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                  @RequestParam(required = false) List<String> uris,
                                  @RequestParam(defaultValue = "false") boolean unique) {
        log.debug("Getting stats");
        return service.get(start, end, uris, unique);
    }
}