package ru.practicum.ewm.statistic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.event.model.Event;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticServiceImpl implements StatisticService {
    private final StatsClient statsClient;
    private final String appName;

    @Autowired
    public StatisticServiceImpl(@Value("${stats-server.url}") String url,
                                @Value("${application.name}") String appName) {
        this.statsClient = new StatsClient(url);
        this.appName = appName;
    }


    @Override
    public void addView(HttpServletRequest request) {
        statsClient.create(EndpointHitDto.builder()
                .app(appName)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    public Map<Long, Long> getStatsEvents(List<Event> events) {
        List<Long> ids = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        LocalDateTime start = events.get(0).getCreatedOn();
        LocalDateTime end = LocalDateTime.now();
        String eventsUri = "/events/";
        List<String> uris = ids.stream()
                .map(id -> eventsUri + id)
                .collect(Collectors.toList());
        List<ViewStatsDto> views = statsClient.getStats(start, end, uris, true);

        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStatsDto view : views) {
            String uri = view.getUri();
            viewsMap.put(Long.parseLong(uri.substring(eventsUri.length())), view.getHits());
        }
        return viewsMap;
    }
}
