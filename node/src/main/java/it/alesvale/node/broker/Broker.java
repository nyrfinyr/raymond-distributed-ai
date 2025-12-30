package it.alesvale.node.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import it.alesvale.node.data.Dto;

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

    public void publishId(Dto.NodeId nodeId, String subject) throws JsonProcessingException {
        brokerConnection.publish(subject, mapper.writeValueAsBytes(nodeId));
    }

    public void publishEvent(Dto.NodeEvent nodeEvent) throws JsonProcessingException {
        brokerConnection.publish("dashboard", mapper.writeValueAsBytes(nodeEvent));
    }
}
