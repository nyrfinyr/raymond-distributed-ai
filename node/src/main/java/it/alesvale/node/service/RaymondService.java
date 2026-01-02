package it.alesvale.node.service;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RaymondService implements AgentService{

    private final NodeState nodeState;
    private final Broker broker;
    private final SocketManager socketManager;

    public RaymondService(NodeState nodeState,
                          Broker broker,
                          SocketManager socketManager){
        this.nodeState = nodeState;
        this.broker = broker;
        this.socketManager = socketManager;
        this.socketManager.setOnReceive(this::onReceive);
    }

    public void start(){
        log.info("[{}] RaymondService start called", nodeState.getId().getHumanReadableId());
        sendIAmChildEvent();
    }

    private void sendIAmChildEvent(){

        if(nodeState.getParent() == null)
            return;

        try {
            Dto.RaymondEvent raymondEvent = Dto.RaymondEvent.builder()
                    .nodeId(nodeState.getId())
                    .eventType(Dto.RaymondEventType.I_AM_CHILD)
                    .build();

            socketManager.send(raymondEvent, nodeState.getParent().address());
            broker.publishInfoMessage(nodeState.getId(),
                    String.format("I_AM_CHILD sent to %s", nodeState.getParent().getHumanReadableId()));
            log.info("[{}] I_AM_CHILD event sent", nodeState.getId().getHumanReadableId());
        }catch(Exception e){
            log.error("[{}] Error during send child event: ",nodeState.getId().getHumanReadableId(), e);
        }
    }

    private void onReceive(Dto.RaymondEvent raymondEvent){
        switch(raymondEvent.eventType()){
            case I_AM_CHILD -> childEvent(raymondEvent);
        }
    }

    private void childEvent(Dto.RaymondEvent raymondEvent){
        nodeState.addChildren(raymondEvent.nodeId());
        log.info("[{}] Child added: {}",
                nodeState.getId().getHumanReadableId(), raymondEvent.nodeId());
        broker.publishInfoMessage(nodeState.getId(),
                String.format("Child added: %s", raymondEvent.nodeId().getHumanReadableId()));
    }
}