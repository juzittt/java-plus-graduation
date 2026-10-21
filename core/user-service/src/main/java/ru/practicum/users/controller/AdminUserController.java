package ru.practicum.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.users.dto.NewUserRequest;
import ru.practicum.users.dto.UserDto;
import ru.practicum.users.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        log.info("POST /admin/users: creating user with email={}", newUserRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(newUserRequest));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @PositiveOrZero Integer size) {
        log.info("GET /admin/users: ids={}, from={}, size={}", ids, from, size);
        return ResponseEntity.ok(userService.getUsers(ids, from, size));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("DELETE /admin/users/{}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ids")
    public ResponseEntity<List<UserShortDto>> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        log.info("GET /admin/users/ids: ids={}", ids);
        return ResponseEntity.ok(userService.getUsersShortDtoByIds(ids));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserShortDto> getUserById(@PathVariable Long userId) {
        log.info("GET /admin/users/{}", userId);
        return ResponseEntity.ok(userService.getUserShortDtoById(userId));
    }
}