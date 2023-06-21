package ru.practicum.ewm.location.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationDto {
    @NotNull
    private Float lat;
    @NotNull
    private Float lon;
}
