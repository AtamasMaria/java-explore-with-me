package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.EventRequestStatusUpdate;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    EventRequestStatusUpdateResult changeStatusRequest(Long userId, Long eventId, EventRequestStatusUpdate statusUpdate);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addUserRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequestOwner(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsUser(Long userId, Long eventId);
}