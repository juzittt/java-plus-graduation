/*
package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.events.dto.EndpointHitDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {

    @PostMapping("/hit")
    ResponseEntity<Object> saveHit(@RequestBody EndpointHitDto hitDto);

    @GetMapping("/stats")
    ResponseEntity<Object> getStats(@RequestParam("start") String start,
                                    @RequestParam("end") String end,
                                    @RequestParam(value = "uris", required = false) List<String> uris,
                                    @RequestParam("unique") Boolean unique);
}*/
