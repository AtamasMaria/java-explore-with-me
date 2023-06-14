package ru.practicum.ewm.compilation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class PublicCompilationController {

    private final CompilationService compilationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CompilationDto> getAllCompilations(@RequestParam(required = false) Boolean pinned,
                                                   @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                                   @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("GET-Получение подборок событий.");
        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        return compilationService.getAllCompilations(pinned, page);
    }

    @GetMapping("{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto getCompilationById(@PathVariable @Positive Long compId) {
        log.info("GET-Получение подборки событий по его идентификатору.");
        return compilationService.getCompilationById(compId);
    }
}
