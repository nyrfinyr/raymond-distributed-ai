package it.alesvale.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;

public class Utils {

    @Getter
    private static final ObjectMapper mapper = createObjectMapper();

    public static String generateNodeId(){
        return java.util.UUID.randomUUID().toString();
    }

    private static ObjectMapper createObjectMapper(){
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
