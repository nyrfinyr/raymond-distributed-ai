package it.alesvale.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.IOException;

public class BrokerPublisher {

    private final Connection brokerConnection;
    private final ObjectMapper mapper;

    public BrokerPublisher() {
        try {
            this.brokerConnection = Nats.connect("nats://localhost:4222");
            this.mapper = new ObjectMapper();
        }catch( Exception e ){
            throw new RuntimeException(e);
        }
    }

    public void publish(Dto.NodeEvent nodeEvent) throws IOException {
        brokerConnection.publish("updates", mapper.writeValueAsBytes(nodeEvent));
    }
}
