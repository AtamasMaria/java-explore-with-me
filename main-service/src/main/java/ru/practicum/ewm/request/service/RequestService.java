package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto addUserRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequestOwner(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsUser(Long userId);
}