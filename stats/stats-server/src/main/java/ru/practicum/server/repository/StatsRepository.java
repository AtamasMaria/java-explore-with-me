package ru.practicum.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.model.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    @Query("SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, count (eh.ip)) " +
            "FROM EndpointHit AS eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri in :uris " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY count (eh.ip) DESC ")
    List<ViewStats> getAllEndpointHitsByUriIn(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, count (DISTINCT eh.ip)) " +
            "FROM EndpointHit AS eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri in :uris " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY count (eh.ip) DESC ")
    List<ViewStats> getAllUniqueEndpointHitByUriIn(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, count (eh.ip)) " +
            "FROM EndpointHit AS eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "GROUP BY  eh.app, eh.uri " +
            "ORDER BY count (eh.ip) DESC ")
    List<ViewStats> getAllEndpointHit(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, count (DISTINCT eh.ip)) " +
            "FROM EndpointHit AS eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "GROUP BY  eh.app, eh.uri " +
            "ORDER BY count (eh.ip) DESC ")
    List<ViewStats> getAllUniqueEndpointHit(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
