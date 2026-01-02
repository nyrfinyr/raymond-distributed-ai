package it.alesvale.node.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import it.alesvale.node.Utils;
import it.alesvale.node.data.Dto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Broker {

    private final Connection brokerConnection;
    private final ObjectMapper mapper;

    public Broker() {
        try {
            String brokerUrl = System.getenv("BROKER_URL");
            this.brokerConnection = Nats.connect(brokerUrl);
            this.mapper = Utils.getMapper();
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

    public void publishInfoMessage(Dto.NodeId nodeId, String message) {

        try {
            Dto.NodeEvent nodeEvent = Dto.NodeEvent.builder()
                    .nodeId(nodeId)
                    .eventType(Dto.NodeEventType.NODE_INFO)
                    .message(message)
                    .timestamp(java.time.Instant.now())
                    .build();

            brokerConnection.publish("dashboard", mapper.writeValueAsBytes(nodeEvent));
        }catch(JsonProcessingException e){
            log.error("Error while publishing info message: ", e);
        }
    }
}
