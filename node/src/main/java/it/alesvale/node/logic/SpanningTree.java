package it.alesvale.node.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SpanningTree {

    private Broker broker;
    private NodeState nodeState;
    private ObjectMapper mapper;
    private Dispatcher dispatcher;
    private ScheduledExecutorService scheduler;
    private final String SUBJECT_NAME = "spanning-tree";

    public SpanningTree(Broker broker, NodeState nodeState){
        this.broker = broker;
        this.nodeState = nodeState;
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start(){

        try {
            log.info("[{}] Starting spanning tree algorithm",
                    nodeState.getId().getHumanReadableId());

            if (nodeState.isLeader()) {
                log.info("[{}] I'm leader, starting periodic announcement", nodeState.getId().getHumanReadableId());
                scheduler.scheduleAtFixedRate(this::periodicAnnouncement, 0, 1500, TimeUnit.MILLISECONDS);
            }else{
                this.dispatcher = broker.createDispatcher(msg -> handleSpanningTreeEvent(msg, nodeState));
                this.dispatcher.subscribe(SUBJECT_NAME, "orphans");
                log.info("[{}] Subscribed to spanning tree events", nodeState.getId().getHumanReadableId());
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    private void periodicAnnouncement(){
        try {
            broker.publishId(nodeState.getId(), SUBJECT_NAME);
            log.debug("[{}] Broadcasting leader ID for Spanning Tree...", nodeState.getId().getHumanReadableId());
        } catch (Exception e) {
            log.error("Error publishing leader ID", e);
        }
    }

    private void handleSpanningTreeEvent(Message message, NodeState nodeState){
        try {
            Dto.NodeId nodeId = mapper.readValue(message.getData(), Dto.NodeId.class);
            log.info("[{}] Received spanning tree event from {}",
                    nodeState.getId().getHumanReadableId(), nodeId.getHumanReadableId());

            if (nodeId.equals(nodeState.getId())) {
                log.info("[{}] Ignoring my own spanning tree event", nodeState.getId().getHumanReadableId());
                return;
            }

            nodeState.setParent(nodeId);
            log.info("[{}] Parent set: {}", nodeState.getId().getHumanReadableId(), nodeId.getHumanReadableId());

            if (this.dispatcher != null) {
                log.info("[{}] Unsubscribing from spanning tree events", nodeState.getId().getHumanReadableId());
                this.dispatcher.unsubscribe(SUBJECT_NAME);
            }

            scheduler.scheduleAtFixedRate(this::periodicAnnouncement, 500, 2000, TimeUnit.MILLISECONDS);
            log.info("[{}] Publishing my id to spanning tree", nodeState.getId().getHumanReadableId());
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
