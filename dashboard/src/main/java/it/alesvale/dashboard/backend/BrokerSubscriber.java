package it.alesvale.dashboard.backend;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import it.alesvale.dashboard.dto.Dto;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Consumer;

@Service
public class BrokerSubscriber {

    private final Connection brokerConnection;
    private final ObjectMapper mapper;

    public BrokerSubscriber(Connection brokerConnection,
                            ObjectMapper mapper) {
        this.brokerConnection = brokerConnection;
        this.mapper = mapper;
    }

    public Dispatcher getDispatcher(Consumer<Dto.NodeEvent> consumer) {
        Dispatcher dispatcher = brokerConnection.createDispatcher((msg) -> {
           Dto.NodeEvent nodeEvent = mapper.readValue(msg.getData(), Dto.NodeEvent.class);
           consumer.accept(nodeEvent);
        });
        dispatcher.subscribe("updates.>");
        return dispatcher;
    }

}
