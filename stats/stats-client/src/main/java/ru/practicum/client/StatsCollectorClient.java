package ru.practicum.client;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.grpc.*;


import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class StatsCollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendAction(Long userId, Long eventId, ActionTypeProto actionType, LocalDateTime timestamp) {
        Timestamp protoTimestamp = Timestamp.newBuilder()
                .setSeconds(timestamp.toEpochSecond(ZoneOffset.UTC))
                .setNanos(timestamp.getNano())
                .build();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(protoTimestamp)
                .build();

        collectorStub.collectUserAction(request);
    }
}