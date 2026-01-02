package it.alesvale.dashboard.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.alesvale.dashboard.dto.Dto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class Beans {

    List<Dto.NodeEvent> eventLogs = new ArrayList<>();

    @Bean
    public ObjectMapper getMapper(){
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = "eventLogs")
    public List<Dto.NodeEvent> getEventLogs(){
        return this.eventLogs;
    }

}
