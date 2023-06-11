package ru.practicum.ewm.event.model;

import lombok.*;

import javax.persistence.Embeddable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class Location {
    private Float lat;
    private Float lon;
}

