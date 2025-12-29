package it.alesvale.node;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import it.alesvale.node.logic.LeaderElection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NodeApplication {

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();
        NodeState state = new NodeState();

        LeaderElection leaderElection = new LeaderElection(state, broker);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(() -> sendAliveEvent(broker, state), 0, 10, TimeUnit.SECONDS);

        leaderElection.start();
    }

    private static void sendAliveEvent(Broker broker, NodeState state){
        try {
            Dto.NodeEvent aliveEvent = Dto.NodeEvent.builder()
                    .nodeId(state.getId())
                    .eventType(Dto.NodeEventType.I_AM_ALIVE)
                    .status(state.getStatus())
                    .edgeTo(state.getEdgeTo())
                    .leader(state.isLeader())
                    .build();
            broker.publishEvent(aliveEvent);
            log.info("[Node {}] Alive event published: {}", state.getId(), aliveEvent);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

}
