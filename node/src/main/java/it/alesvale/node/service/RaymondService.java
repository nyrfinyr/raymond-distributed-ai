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
            sendIAmChildEvent();
        }

        // Start the simulation loop in a separate thread so it doesn't block the receiver or main thread
        new Thread(this::loop).start();
    }

    private void loop() {
        while (running) {
            try {
                // 1. Simulate work outside Critical Section
                long waitTime = 10000 + random.nextInt(10000);
                log.info("[{}] Working outside CS for {}ms...", nodeState.getHumanReadableId(), waitTime);
                Thread.sleep(waitTime);

                // 2. Ask for Critical Section [cite: 143]
                log.info("[{}] Requesting Critical Section...", nodeState.getHumanReadableId());
                askForCriticalSection();

                // 3. Wait until granted
                synchronized (nodeState) {
                    while (nodeState.getStatus() != Dto.NodeStatus.CRITICAL) {
                        nodeState.wait(); // Wait for notification from ASSIGN_PRIVILEGE
                    }
                }

                // 4. Critical Section
                log.info("[{}] >>> ENTERING CRITICAL SECTION <<<", nodeState.getHumanReadableId());
                broker.publishInfoMessage(nodeState.getId(), "EXECUTING CRITICAL SECTION");

                // Simulate CS work
                Thread.sleep(15000 + random.nextInt(3000));

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

    private synchronized void sendIAmChildEvent(){
        try {
            Dto.RaymondEvent raymondEvent = Dto.RaymondEvent.builder()
                    .nodeId(nodeState.getId())
                    .eventType(Dto.RaymondEventType.I_AM_CHILD)
                    .build();
            socketManager.send(raymondEvent, nodeState.getParent().address());
            broker.publishInfoMessage(nodeState.getId(),
                    String.format("I_AM_CHILD sent to %s", nodeState.getParent().getHumanReadableId()));
            log.info("[{}] I_AM_CHILD event sent", nodeState.getHumanReadableId());
        }catch(Exception e){
            log.error("[{}] Error during send child event: ",nodeState.getHumanReadableId(), e);
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
        if(!nodeState.isHolder()){
            return;
        }
        if(nodeState.isUsing()){
            return;
        }
        if(nodeState.isQueueEmpty()){
            return;
        }

        // Logic [cite: 116, 117]
        Dto.NodeId headRequest = nodeState.dequeueRequest();
        nodeState.setHolder(headRequest);
        nodeState.setAsked(false);

        // [cite: 118, 120]
        if(nodeState.isHolder()){
            nodeState.setUsing(true);
            nodeState.setStatus(Dto.NodeStatus.CRITICAL);
            broker.publishInfoMessage(nodeState.getId(), "PRIVILEGE ACQUIRED: ENTERING CS");

            // Notify the loop thread that we are now Critical
            synchronized (nodeState) {
                nodeState.notifyAll();
            }
            return;
        }

        Dto.RaymondEvent raymondEvent = Dto.RaymondEvent.builder()
                .nodeId(nodeState.getId()) // Sender ID
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
        if(nodeState.isHolder()){
            return;
        }
        if(nodeState.isQueueEmpty()){
            return;
        }
        if(nodeState.hasAlreadyAsked()) {
            return;
        }

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
            case I_AM_CHILD -> onChildEvent(raymondEvent);
            case REQUEST -> onRequestEvent(raymondEvent);
            case PRIVILEGE -> onPrivilegeEvent(raymondEvent);
        }
    }

    private synchronized void onChildEvent(Dto.RaymondEvent raymondEvent){
        nodeState.addChildren(raymondEvent.nodeId());
        log.info("[{}] Child added: {}",
                nodeState.getHumanReadableId(), raymondEvent.nodeId());
        broker.publishInfoMessage(nodeState.getId(),
                String.format("Child added: %s", raymondEvent.nodeId().getHumanReadableId()));
    }

    private synchronized void onRequestEvent(Dto.RaymondEvent raymondEvent){
        // Event: The node receives a REQUEST message [cite: 147, 148]
        this.nodeState.enqueueRequest(raymondEvent.nodeId());
        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }

    private synchronized void onPrivilegeEvent(Dto.RaymondEvent raymondEvent){
        // Event: The node receives a PRIVILEGE message [cite: 151, 152]
        this.nodeState.setHolder(nodeState.getId());
        ASSIGN_PRIVILEGE();
        MAKE_REQUEST();
    }
}