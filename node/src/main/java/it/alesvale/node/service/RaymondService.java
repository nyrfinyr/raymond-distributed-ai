package it.alesvale.node.service;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class RaymondService implements AgentService{

    private final NodeState nodeState;
    private final Broker broker;
    private final SocketManager socketManager;
    private boolean running;
    private Random random;

    public RaymondService(NodeState nodeState,
                          Broker broker,
                          SocketManager socketManager){
        this.nodeState = nodeState;
        this.broker = broker;
        this.socketManager = socketManager;
        this.socketManager.setOnReceive(this::onRaymondEvent);
        this.running = true;
        this.random = new Random();
    }

    public void start(){
        log.info("[{}] RaymondService start called", nodeState.getHumanReadableId());

        // Initialization [cite: 356, 357]
        if(nodeState.isLeader()){
            this.nodeState.getRaymondState().setUsing(false);
            this.nodeState.getRaymondState().setHolder(this.nodeState.getId());
            this.nodeState.setStatus(Dto.NodeStatus.IDLE);
        } else {
            // Non-leaders point holder toward parent [cite: 358]
            this.nodeState.getRaymondState().setHolder(this.nodeState.getParent());
        }

        new Thread(this::loop).start();
    }

    private void loop() {
        while (running) {
            try {
                long waitTime = 20000 + random.nextInt(20000); // 20-40 seconds
                log.info("[{}] Working outside CS for {}ms...", nodeState.getHumanReadableId(), waitTime);
                Thread.sleep(waitTime);

                // 2. Ask for Critical Section [cite: 143]
                log.info("[{}] Requesting Critical Section...", nodeState.getHumanReadableId());
                askForCriticalSection();

                // 3. Wait until granted
                synchronized (nodeState) {
                    while (nodeState.getStatus() != Dto.NodeStatus.CRITICAL) {
                        nodeState.wait();
                    }
                }

                // 4. Critical Section
                log.info("[{}] >>> ENTERING CRITICAL SECTION <<<", nodeState.getHumanReadableId());
                broker.publishInfoMessage(nodeState.getId(), "EXECUTING CRITICAL SECTION");

                // 2-4 seconds in CS
                Thread.sleep(2000 + random.nextInt(2000));

                // 5. Exit Critical Section [cite: 155]
                log.info("[{}] <<< EXITING CRITICAL SECTION >>>", nodeState.getHumanReadableId());
                exitFromCriticalSection();

            } catch (InterruptedException e) {
                log.error("Loop interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in loop", e);
            }
        }
    }

    private synchronized void askForCriticalSection(){
        // Event: The node wishes to enter the critical section [cite: 143]
        this.nodeState.enqueueRequest(nodeState.getId());
        this.nodeState.setStatus(Dto.NodeStatus.REQUESTING);
        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }

    private synchronized void exitFromCriticalSection(){
        // Event: The node exits the critical section [cite: 155]
        this.nodeState.setUsing(false);
        this.nodeState.setStatus(Dto.NodeStatus.IDLE);
        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }

    private synchronized void ASSIGN_PRIVILEGE(){
        // Preconditions
        if(!nodeState.isHolder() || nodeState.isUsing() || nodeState.isQueueEmpty())
            return;

        // Logic [cite: 116, 117]
        Dto.NodeId headRequest = nodeState.dequeueRequest();
        nodeState.setHolder(headRequest);
        nodeState.setAsked(false);

        // [cite: 118, 120]
        if(nodeState.isHolder()){
            nodeState.setUsing(true);
            nodeState.setStatus(Dto.NodeStatus.CRITICAL);
            broker.publishInfoMessage(nodeState.getId(), "PRIVILEGE ACQUIRED: ENTERING CS");

            synchronized (nodeState) {
                nodeState.notifyAll();
            }
            return;
        }

        // Visualization purposes
        DashboardUpdateUtils.sendAliveEvent(broker, nodeState);

        Dto.RaymondEvent raymondEvent = Dto.RaymondEvent.builder()
                .nodeId(nodeState.getId())
                .eventType(Dto.RaymondEventType.PRIVILEGE)
                .build();

        Dto.SocketAddress holderAddress = nodeState.getHolderAddress()
                .orElseThrow(() -> new RuntimeException(String.format("[%s] holder is null", nodeState.getHumanReadableId())));

        socketManager.send(raymondEvent, holderAddress);
        broker.publishInfoMessage(nodeState.getId(),
                String.format("PRIVILEGE sent to %s", nodeState.getHolder().getHumanReadableId()));
    }

    private synchronized void MAKE_REQUEST(){
        // Preconditions
        if(nodeState.isHolder() || nodeState.isQueueEmpty() || nodeState.hasAlreadyAsked())
            return;

        // Logic [cite: 135]
        Dto.RaymondEvent raymondEvent = Dto.RaymondEvent.builder()
                .nodeId(nodeState.getId())
                .eventType(Dto.RaymondEventType.REQUEST)
                .build();

        Dto.SocketAddress holderAddress = nodeState.getHolderAddress()
                .orElseThrow(() -> new RuntimeException(String.format("[%s] holder is null", nodeState.getHumanReadableId())));

        socketManager.send(raymondEvent, holderAddress);
        this.nodeState.setAsked(true);

        broker.publishInfoMessage(nodeState.getId(),
                String.format("REQUEST sent to %s", nodeState.getHolder().getHumanReadableId()));
    }

    private synchronized void onRaymondEvent(Dto.RaymondEvent raymondEvent){
        switch(raymondEvent.eventType()){
            case REQUEST -> onRequestEvent(raymondEvent);
            case PRIVILEGE -> onPrivilegeEvent();
        }
    }

    private synchronized void onRequestEvent(Dto.RaymondEvent raymondEvent){
        // Event: The node receives a REQUEST message [cite: 147, 148]
        this.nodeState.enqueueRequest(raymondEvent.nodeId());
        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }

    private synchronized void onPrivilegeEvent(){
        this.nodeState.setHolder(nodeState.getId());

        DashboardUpdateUtils.sendAliveEvent(broker, nodeState);
        waitASecond();

        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }

    private void waitASecond(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}