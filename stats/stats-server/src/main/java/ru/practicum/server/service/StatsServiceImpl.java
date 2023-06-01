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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Transactional
    @Override
    public void create(EndpointHitDto endpointHitDto) {
        repository.save(EndpointHitMapper.toEndpointHit(endpointHitDto));
    }

    @Override
    public List<ViewStatsDto> get(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (unique) {
            List<ViewStats> statsListUnique = repository.getUnique(startTime, endTime, uris);
            return toDto(statsListUnique);
        } else {
            List<ViewStats> statsList = repository.get(startTime, endTime, uris);
            return toDto(statsList);
        }
    }

    private List<ViewStatsDto> toDto(List<ViewStats> list) {
        if (!list.isEmpty()) {
            return list.stream()
                    .map(ViewStatsMapper::toViewStatsDto)
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }
}


