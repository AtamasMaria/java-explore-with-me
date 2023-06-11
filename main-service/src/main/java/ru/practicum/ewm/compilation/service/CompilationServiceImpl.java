package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationDto;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.dto.EventDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto compilationDto) {
        if (compilationDto.getEvents() == null || compilationDto.getEvents().isEmpty()) {
            Compilation compilation = CompilationMapper.mapCompilationDtoToCompilation(compilationDto, new ArrayList<>());
            return compilationMapper.toCompilationDto(compilationRepository.save(compilation));
        }
        List<Event> events = eventRepository.findAllByIdIn(compilationDto.getEvents());
        if (compilationDto.getEvents().size() != events.size()) {
            throw new NotFoundException("No events found");
        }
        Compilation compilation = compilationMapper.mapCompilationDtoToCompilation(compilationDto, events);
        return compilationMapper.toCompilationDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void deleteCompById(Long compId) {
        Compilation compilation = getCompById(compId);
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationDto updateComp) {
        Compilation compilation = getCompById(compId);
        if (updateComp.getTitle() != null && !updateComp.getTitle().isBlank()) {
            compilation.setTitle(updateComp.getTitle());
        }
        if (updateComp.getPinned() != null) {
            compilation.setPinned(updateComp.getPinned());
        }
        if (updateComp.getEvents() != null) {
            compilation.setEvents(eventRepository.findAllByIdIn(updateComp.getEvents()));
        }
        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable;
        if (pinned == null) {
            pageable = PageRequest.of(from, size);
        } else {
            pageable = PageRequest.of(from, size, Sort.Direction.DESC, "id");
        }
        return compilationRepository.findAllByPinned(pinned, pageable).stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = getCompById(compId);
        List<EventDto> events = compilation.getEvents()
                .stream()
                .map(EventMapper::toEventDto)
                .collect(Collectors.toList());
        CompilationDto dto = compilationMapper.toCompilationDto(compilation);
        dto.setEvents(events);
        return dto;
    }

    private Compilation getCompById(Long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException(String.format("Подборка с id={} не найдена.", compId)));
    }
}

