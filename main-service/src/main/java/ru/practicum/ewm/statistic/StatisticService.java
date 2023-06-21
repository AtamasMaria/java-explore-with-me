package ru.practicum.ewm.statistic;

import ru.practicum.ewm.event.model.Event;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface StatisticService {
    void addView(HttpServletRequest request);

    Map<Long, Long> getStatsEvents(List<Event> events);
}
