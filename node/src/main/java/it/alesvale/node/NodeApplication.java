package it.alesvale.node;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.io.IOException;

public class NodeApplication {

    private static final ObjectIdGenerators.UUIDGenerator uuidGenerator = new ObjectIdGenerators.UUIDGenerator();
    private static final BrokerPublisher publisher = new BrokerPublisher();
    private static final String NODE_ID = uuidGenerator.generateId(null).toString();

    public static void main(String[] args) {
        try {
            Dto.NodeEvent aliveEvent = new Dto.NodeEvent(NODE_ID, Dto.NodeEventType.I_AM_ALIVE);
            publisher.publish(aliveEvent);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }



}
