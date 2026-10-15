package client;

import ewm.dto.EndpointHitDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String statsServerUrl;

    public StatsClient(@Value("${stats-server.url:http://stats-server}") String statsServerUrl,
                       @LoadBalanced RestTemplate restTemplate) {
        this.statsServerUrl = statsServerUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<Object> saveHit(EndpointHitDto hitDto) {
        return restTemplate.postForEntity(statsServerUrl + "/hit", hitDto, Object.class);
    }

    public ResponseEntity<Object> getStats(LocalDateTime start,
                                           LocalDateTime end,
                                           List<String> uris,
                                           Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(statsServerUrl + "/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }

        return restTemplate.getForEntity(builder.toUriString(), Object.class);
    }

    @Configuration
    static class RestTemplateConfig {
        @Bean
        @LoadBalanced
        public RestTemplate loadBalancedRestTemplate() {
            return new RestTemplate();
        }
    }
}