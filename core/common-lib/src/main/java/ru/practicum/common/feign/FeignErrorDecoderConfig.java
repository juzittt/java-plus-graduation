package ru.practicum.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.common.exception.ApiError;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ValidationException;

import java.io.InputStream;

@Configuration
public class FeignErrorDecoderConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        return (methodKey, response) -> {
            String message = readMessage(objectMapper, response);
            switch (response.status()) {
                case 404: return new NotFoundException(message);
                case 409: return new ConflictException(message);
                case 400: return new ValidationException(message);
                default:  return defaultDecoder.decode(methodKey, response);
            }
        };
    }

    private String readMessage(ObjectMapper objectMapper, Response response) {
        if (response.body() == null) {
            return "Remote error: " + response.status();
        }
        try (InputStream is = response.body().asInputStream()) {
            ApiError apiError = objectMapper.readValue(is, ApiError.class);
            return apiError.getMessage() != null ? apiError.getMessage() : "Remote error: " + response.status();
        } catch (Exception e) {
            return "Remote error: " + response.status();
        }
    }
}
