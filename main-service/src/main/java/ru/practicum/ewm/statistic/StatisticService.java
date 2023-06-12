package ru.practicum.ewm.statistic;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.event.model.Event;

import javax.servlet.http.HttpServletRequest;

public interface StatisticService {
    void addHit(HttpServletRequest request);

    void setEventViews(Event event);
}
