package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> getAllByInitiatorId(Long userId, Pageable page);
    List<Event> findByCategory(Category category);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    List<Event> findAllByIdIn(List<Long> eventIds);

    Boolean existsByCategoryId(Long catId);

    Optional<Event> findByIdAndPublishedOnIsNotNull(Long id);

    @Query("SELECT e FROM Event as e " +
            "JOIN FETCH e.initiator " +
            "JOIN FETCH e.category " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (e.eventDate BETWEEN :start AND :end)")
    List<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<EventState> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  Pageable pageable);
}