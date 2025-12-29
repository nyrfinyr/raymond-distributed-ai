package it.alesvale.node.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import it.alesvale.node.data.Dto;

import java.io.IOException;

public class Broker {

    private final Connection brokerConnection;
    private final ObjectMapper mapper;

    public Broker() {
        try {
            String brokerUrl = System.getenv("BROKER_URL");
            this.brokerConnection = Nats.connect(brokerUrl);
            this.mapper = new ObjectMapper();
        }catch( Exception e ){
            throw new RuntimeException(e);
        }
    }

    public Dispatcher createDispatcher(MessageHandler handler){
        return brokerConnection.createDispatcher(handler);
    }

    public void publishId(String nodeId, String subject){
        brokerConnection.publish(subject, nodeId.getBytes());
    }

    public void publishEvent(Dto.NodeEvent nodeEvent) throws IOException {
        brokerConnection.publish("dashboard", mapper.writeValueAsBytes(nodeEvent));
    }

}
