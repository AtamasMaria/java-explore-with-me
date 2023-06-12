package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.dto.EventRequestCountDto;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {

    /*@Query("SELECT NEW ru.practicum.ewm.request.dto.EventRequestCountDto(r.event.id, COUNT(*), r.status) " +
            "FROM Request as r " +
            "WHERE (r.event.id IN :eventIds) " +
            "AND (r.status = :status) " +
            "GROUP BY r.id")
    List<EventRequestCountDto> findEventRequest(@Param("eventIds") Set<Long> eventIds,
                                                @Param("status") RequestStatus status);*/

    List<Request> findAllByIdInAndStatus(Set<Long> eventId, RequestStatus status);

    List<Request> findAllByRequesterId(Long requesterId);

    Optional<Request> findByRequesterIdAndEventId(Long userId, Long eventId);
}
