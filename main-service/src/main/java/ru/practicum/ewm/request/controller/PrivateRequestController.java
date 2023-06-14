package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping("{userId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserRequests(@PathVariable(name = "userId") @Positive Long userId) {
        return requestService.getUserRequests(userId);
    }

    @PostMapping("{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addUserRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                  @RequestParam @Positive Long eventId) {
        return requestService.addUserRequest(userId, eventId);
    }

    @PatchMapping("{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                 @PathVariable(name = "requestId")  @Positive Long requestId) {
        return requestService.cancelRequestOwner(userId, requestId);
    }
}
