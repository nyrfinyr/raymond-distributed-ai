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
            String brokerUrl = System.getenv("BROKER_URL");
            this.brokerConnection = Nats.connect(brokerUrl);
            this.mapper = new ObjectMapper();
        }catch( Exception e ){
            throw new RuntimeException(e);
        }
    }

    public void publish(Dto.NodeEvent nodeEvent) throws IOException {
        brokerConnection.publish("updates", mapper.writeValueAsBytes(nodeEvent));
    }
}
