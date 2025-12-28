package it.alesvale.node;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NodeApplication {

    private static final BrokerPublisher publisher = new BrokerPublisher();
    private static final NodeState state = new NodeState();

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(NodeApplication::sendAliveEvent, 0, 10, TimeUnit.SECONDS);
    }

    private static void sendAliveEvent(){
        try {
            Dto.NodeEvent aliveEvent = Dto.NodeEvent.builder()
                    .nodeId(state.getId())
                    .eventType(Dto.NodeEventType.I_AM_ALIVE)
                    .status(state.getStatus())
                    .edgeTo(state.getEdgeTo())
                    .build();
            publisher.publish(aliveEvent);
            log.info("[Node {}] Alive event published: {}", state.getId(), aliveEvent);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

}
