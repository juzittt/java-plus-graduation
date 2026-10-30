package ru.practicum.kafka;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import java.io.IOException;
import java.util.Map;

public class AvroDeserializer<T extends SpecificRecord> implements Deserializer<T> {
    private Class<T> targetType;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.targetType = (Class<T>) configs.get("spring.json.value.default.type");
        if (this.targetType == null && !isKey) {
            throw new IllegalArgumentException("Target type must be provided for Avro deserialization");
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(targetType);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing Avro message", e);
        }
    }
}