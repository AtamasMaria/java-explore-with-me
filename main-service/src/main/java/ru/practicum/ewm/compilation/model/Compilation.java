package ru.practicum.ewm.compilation.model;

import lombok.*;
import ru.practicum.ewm.event.model.Event;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "compilations")
public class Compilation {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;
 @Column(nullable = false)
 private Boolean pinned;
 @Column(nullable = false, unique = true)
 private String title;
 @ManyToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
 @JoinTable(name = "compilation_events", joinColumns = {@JoinColumn(name = "compilation_id")},
         inverseJoinColumns = @JoinColumn(name = "event_id"))
 private List<Event> events;
}
