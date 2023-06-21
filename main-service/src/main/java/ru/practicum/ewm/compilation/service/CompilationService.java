package ru.practicum.ewm.compilation.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationDto;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto compilationDto);

    void deleteCompById(Long compId);

    CompilationDto updateCompilation(Long compId, UpdateCompilationDto updateReq);

    List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable);

    CompilationDto getCompilationById(Long compId);
}
