package ru.practicum.recommendation.collector.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.*;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    private static final long SECONDS_TO_MILLIS = 1000L;
    private static final long NANOS_TO_MILLIS = 1_000_000L;
    private static final String TOPIC_USER_ACTIONS = "stats.user-actions.v1";

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Received user action: userId={}, eventId={}, action={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        try {
            ActionTypeAvro avroActionType = switch (request.getActionType()) {
                case ACTION_VIEW -> ActionTypeAvro.VIEW;
                case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
                case ACTION_LIKE -> ActionTypeAvro.LIKE;
                case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type: " + request.getActionType());
            };

            long timestampMs = request.getTimestamp().getSeconds() * SECONDS_TO_MILLIS +
                    request.getTimestamp().getNanos() / NANOS_TO_MILLIS;

            UserActionAvro avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(avroActionType)
                    .setTimestamp(Instant.ofEpochMilli(timestampMs))
                    .build();

            kafkaTemplate.send(TOPIC_USER_ACTIONS, request.getEventId(), avroMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully sent action to Kafka. Topic: {}, Partition: {}, Offset: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send action to Kafka", ex);
                        }
                    });

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}