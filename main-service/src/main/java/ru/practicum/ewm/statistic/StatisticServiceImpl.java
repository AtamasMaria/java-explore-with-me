package ru.practicum.ewm.statistic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.event.model.Event;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {
    private final StatsClient statsClient = new StatsClient("http://stats-server:9090");

    @Override
    public void addHit(HttpServletRequest request) {
        statsClient.create(
                EndpointHitDto.builder()
                        .app("ewm-service")
                        .timestamp(LocalDateTime.now())
                        .uri(request.getRequestURI())
                        .ip(request.getRemoteAddr())
                        .build());
    }

    @Override
    public void setEventViews(Event event) {
        int views = statsClient.getStats(event.getCreatedOn(),
                LocalDateTime.now(),
                List.of("/events/" + event.getId()), true).size();
        event.setViews((long) views);
    }
}
