package ru.practicum.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.mapper.EndpointHitMapper;
import ru.practicum.server.mapper.ViewStatsMapper;
import ru.practicum.server.model.ViewStats;
import ru.practicum.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Transactional
    @Override
    public void create(EndpointHitDto endpointHitDto) {
        repository.save(EndpointHitMapper.toEndpointHit(endpointHitDto));
    }

    @Override
    public List<ViewStatsDto> get(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (unique) {
            if (!uris.isEmpty()) {
                return listToDto(repository.getAllUniqueEndpointHitByUriIn(start, end, uris));
            }
            return listToDto(repository.getAllUniqueEndpointHit(start, end));
        } else {
            if (!uris.isEmpty()) {
                return listToDto(repository.getAllEndpointHitsByUriIn(start, end, uris));
            }
            return listToDto(repository.getAllEndpointHit(start, end));
        }
    }

    private List<ViewStatsDto> listToDto(List<ViewStats> list) {
            return list.stream()
                    .map(ViewStatsMapper::toViewStatsDto)
                    .collect(Collectors.toList());
    }
}


