package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import javax.validation.constraints.Positive;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping("{userId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserRequests(@PathVariable(name = "userId") @Positive Long userId) {
        log.info("GET-Получение информации о заявках текущего пользователя на участие в чужих событиях.");
        return requestService.getRequestsUser(userId);
    }

    @PostMapping("{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addUserRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                  @RequestParam(name = "eventId") Long eventId) {
        log.info("POST-Добавление запроса от текущего пользователя но участие в событии.");
        return requestService.addUserRequest(userId, eventId);
    }

    @PatchMapping("{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable(name = "userId") @Positive Long userId,
                                                 @PathVariable(name = "requestId")  @Positive Long requestId) {
        log.info("PATCH-Отмена своего запроса на участие в событии.");
        return requestService.cancelRequestOwner(userId, requestId);
    }
}
