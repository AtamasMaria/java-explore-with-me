package ru.practicum.ewm.request.dto;

import lombok.*;
import ru.practicum.ewm.request.model.RequestStatus;

@Setter
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class EventRequestCountDto {
    private Long eventId;
    private Long count;
    private RequestStatus status;
}
