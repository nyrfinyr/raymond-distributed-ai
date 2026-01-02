package it.alesvale.dashboard.backend;

import com.vaadin.flow.shared.Registration;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import it.alesvale.dashboard.dto.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Consumer;

@Service
@Slf4j
public class BrokerSubscriber {

    private final Connection brokerConnection;
    private final ObjectMapper mapper;
    private static final String SUBJECT_NAME = "dashboard";

    public BrokerSubscriber(Connection brokerConnection,
                            ObjectMapper mapper) {
        this.brokerConnection = brokerConnection;
        this.mapper = mapper;
    }

    public Registration getDispatcher(Consumer<Dto.NodeEvent> consumer) {
        Dispatcher dispatcher = brokerConnection.createDispatcher((msg) -> {
           Dto.NodeEvent nodeEvent = mapper.readValue(msg.getData(), Dto.NodeEvent.class);
           log.trace("Received node event: {}", nodeEvent);
           consumer.accept(nodeEvent);
        });
        dispatcher.subscribe(SUBJECT_NAME);
        log.info("Subscribed to {} subject", SUBJECT_NAME);
        return () -> brokerConnection.closeDispatcher(dispatcher);
    }

}
