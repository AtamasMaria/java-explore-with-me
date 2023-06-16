package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCategory(Category category);

    Set<Event> findAllByIdIn(List<Long> eventIds);

    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("SELECT e FROM Event as e " +
            "JOIN FETCH e.initiator " +
            "JOIN FETCH e.category " +
            "WHERE (:users IS NULL OR e.initiator.id IN (:users)) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (e.eventDate BETWEEN :start AND :end)")
    List<Event> findAllByAdmin(@Param("users") List<Long> users,
                               @Param("categories") List<Long> categories,
                               @Param("states") List<EventState> states,
                               @Param("start") LocalDateTime rangeStart,
                               @Param("end") LocalDateTime rangeEnd,
                               Pageable pageable);

    @Query("SELECT e FROM Event as e " +
            "JOIN FETCH e.initiator " +
            "JOIN FETCH e.category " +
            "WHERE (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (:states IS NULL OR e.state = :states) " +
            "AND (e.eventDate BETWEEN :start AND :end) " +
            "AND ((:text IS NULL) " +
            "       OR LOWER(e.description) LIKE LOWER(CONCAT('%',:text,'%')) " +
            "       OR LOWER(e.annotation) LIKE LOWER(CONCAT('%',:text,'%')))")
    List<Event> findAllByUser(@Param("categories") List<Long> categories,
                              @Param("paid") Boolean paid,
                              @Param("states") EventState states,
                              @Param("start") LocalDateTime rangeStart,
                              @Param("end") LocalDateTime rangeEnd,
                              @Param("text") String text,
                              Pageable pageable);
}
