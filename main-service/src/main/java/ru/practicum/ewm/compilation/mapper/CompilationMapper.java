package ru.practicum.ewm.compilation.mapper;

import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompilationMapper {
    public static CompilationDto toCompilationDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(compilation.getEvents().stream()
                        .map(EventMapper::toEventDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Compilation mapCompilationDtoToCompilation(NewCompilationDto newCompilationDto, List<Event> events) {
        List<Event> eventSet = new ArrayList<>();
        for (Long eventId : newCompilationDto.getEvents()) {
            for (Event event : events) {
                if (event.getId().equals(eventId)) {
                    eventSet.add(event);
                    break;
                }
            }
        }
        return Compilation.builder()
                .pinned(newCompilationDto.getPinned())
                .title(newCompilationDto.getTitle())
                .events(eventSet)
                .build();
    }
}
