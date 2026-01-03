package it.alesvale.node.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import it.alesvale.node.Utils;
import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.*;

@Slf4j
public class SpanningTreeService implements AgentService {

    private final Broker broker;
    private final NodeState nodeState;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;
    private final Random random;

    private final String SUBJECT_ANNOUNCE = "spanning-tree.announce";
    private final String SUBJECT_JOINED = "spanning-tree.joined";
    private final String SUBJECT_STABILIZED = "spanning-tree.stabilized";

    private Dispatcher announceDispatcher;

    private ScheduledFuture<?> broadcastTask;
    private ScheduledFuture<?> stabilizationTimer;

    //seconds
    private final long STABILIZATION_TIMEOUT = 20;
    private final long BROADCAST_RATE = 1;

    @Setter
    private Runnable onStabilizedCallback;

    public SpanningTreeService(NodeState nodeState,
                               Broker broker){
        this.broker = broker;
        this.nodeState = nodeState;
        this.mapper = Utils.getMapper();
        this.random = new Random();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void start(){
        try {
            log.info("[{}] Starting spanning tree service", nodeState.getHumanReadableId());

            broker.publishInfoMessage(nodeState.getId(), "Starting building Spanning Tree");

            broker.createDispatcher(msg -> handleStabilization())
                    .subscribe(SUBJECT_STABILIZED);

            if (nodeState.isLeader()) {
                startLeaderLogic();
            } else {
                startOrphanLogic();
            }

        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private void startLeaderLogic() {
        log.info("[{}] I'm LEADER. Starting discovery...", nodeState.getHumanReadableId());

        startBroadcastingIdentity();

        broker.createDispatcher(msg -> resetStabilizationTimer())
                .subscribe(SUBJECT_JOINED);

        resetStabilizationTimer();
    }

    private void startOrphanLogic() {
        this.announceDispatcher = broker.createDispatcher(this::handleParentAnnouncement);
        this.announceDispatcher.subscribe(SUBJECT_ANNOUNCE, "orphans");

        log.info("[{}] I am an orphan, waiting in queue...", nodeState.getHumanReadableId());
    }

    private void resetStabilizationTimer() {
        if (stabilizationTimer != null && !stabilizationTimer.isDone()) {
            stabilizationTimer.cancel(false);
        }

        stabilizationTimer = scheduler.schedule(() -> {
            try {
                log.info("[LEADER] No joins detected for {} seconds. Broadcasting STABILIZED.", STABILIZATION_TIMEOUT);
                broker.publishId(nodeState.getId(), SUBJECT_STABILIZED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, STABILIZATION_TIMEOUT, TimeUnit.SECONDS);
    }

    private void handleParentAnnouncement(Message message) {
        try {
            Dto.NodeId potentialParentId = mapper.readValue(message.getData(), Dto.NodeId.class);

            if (potentialParentId.equals(nodeState.getId()))
                return;

            log.info("[{}] Adopted by parent: {}",
                    nodeState.getHumanReadableId(), potentialParentId.getHumanReadableId());

            nodeState.setParent(potentialParentId);

            if (this.announceDispatcher != null) {
                this.announceDispatcher.unsubscribe(SUBJECT_ANNOUNCE);
            }

            broker.publishId(nodeState.getId(), SUBJECT_JOINED);
            long delay_ms = random.nextInt(500);
            scheduler.schedule(this::startBroadcastingIdentity, delay_ms, TimeUnit.MILLISECONDS);
        } catch(Exception e) {
            log.error("Error handling parent announcement", e);
        }
    }

    private void startBroadcastingIdentity() {
        long delay_s = random.nextInt(2);
        this.broadcastTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                broker.publishId(nodeState.getId(), SUBJECT_ANNOUNCE);
            } catch (Exception e) {
                log.error("Broadcast error", e);
            }
        }, delay_s, BROADCAST_RATE, TimeUnit.SECONDS);
    }

    private void handleStabilization() {
        log.info("[{}] Spanning Tree stabilized ({}s silence)",
                nodeState.getHumanReadableId(), STABILIZATION_TIMEOUT);

        broker.publishInfoMessage(nodeState.getId(), "Spanning Tree stabilized");

        if (broadcastTask != null) {
            broadcastTask.cancel(true);
        }

        if (onStabilizedCallback != null) {
            onStabilizedCallback.run();
        }
    }
}