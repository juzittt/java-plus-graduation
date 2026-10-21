package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/admin/users/{userId}")
    UserShortDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/admin/users/ids")
    List<UserShortDto> getUsersByIds(@RequestParam("ids") List<Long> ids);
}