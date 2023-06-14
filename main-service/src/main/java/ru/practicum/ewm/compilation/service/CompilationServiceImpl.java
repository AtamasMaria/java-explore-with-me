package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationDto;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto compilationDto) {
        if (compilationDto.getEvents() == null || compilationDto.getEvents().isEmpty()) {
            Compilation compilation = CompilationMapper.newCompilationDtoToCompilation(compilationDto, new HashSet<>());
            return CompilationMapper.toCompilationDto(compilationRepository.save(compilation));
        }
        Set<Event> events = eventRepository.findAllByIdIn(compilationDto.getEvents());
        Compilation compilation = CompilationMapper.newCompilationDtoToCompilation(compilationDto, events);
        return CompilationMapper.toCompilationDto(compilationRepository.save(compilation));
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
        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable) {
        List<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).toList();
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }
        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = getCompById(compId);
        return CompilationMapper.toCompilationDto(compilation);
    }

    private Compilation getCompById(Long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException(String.format("Подборка с id={} не найдена.", compId)));
    }
}

