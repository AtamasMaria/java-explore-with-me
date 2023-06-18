package ru.practicum.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.exception.DataException;
import ru.practicum.server.mapper.EndpointHitMapper;
import ru.practicum.server.mapper.ViewStatsMapper;
import ru.practicum.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Override
    @Transactional
    public EndpointHitDto create(EndpointHitDto endpointHitDto) {
        return EndpointHitMapper.toEndpointHitDto(repository.save(EndpointHitMapper.toEndpointHit(endpointHitDto)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> get(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new DataException("Начало мероприятия не может быть позже окончания мероприятия.");
        }
        if (unique) {
            return ViewStatsMapper.listToDto(repository.getAllUniqueEndpointHitByUriIn(start, end, uris));
        } else {
            return ViewStatsMapper.listToDto(repository.getAllEndpointHitsByUriIn(start, end, uris));
        }
    }
}


