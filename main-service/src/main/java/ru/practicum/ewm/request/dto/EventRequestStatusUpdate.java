package ru.practicum.ewm.request.dto;

import lombok.*;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.Set;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdate {
    private Set<Long> requestIds;
    private RequestStatus status;
}