package it.alesvale.node;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import it.alesvale.node.data.StateMachine;
import it.alesvale.node.service.*;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.alesvale.node.service.DashboardUpdateUtils.sendAliveEvent;

@Slf4j
public class NodeApplication {

    public static void main(String[] args) throws UnknownHostException {

        Broker broker = new Broker();
        SocketManager socketManager = new SocketManager();
        Dto.SocketAddress socketAddress = socketManager.getSocketAddress();
        NodeState state = new NodeState(socketAddress);

        log.info("[{}] Socket address: {}", state.getId().getHumanReadableId(), socketAddress);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> sendAliveEvent(broker, state), 0, 1, TimeUnit.SECONDS);

        StateMachine<Dto.AgentState> agentStateMachine = new StateMachine<>();
        LeaderElectionService leaderElectionService = new LeaderElectionService(state, broker);
        SpanningTreeService spanningTreeService = new SpanningTreeService(state, broker);
        RaymondService raymondService = new RaymondService(state, broker, socketManager);

        agentStateMachine.onStateAction(Dto.AgentState.LEADER_ELECTION, leaderElectionService);
        agentStateMachine.onStateAction(Dto.AgentState.BUILDING_SPANNING_TREE, spanningTreeService);
        agentStateMachine.onStateAction(Dto.AgentState.RAYMOND_MUTUAL_EXCLUSION, raymondService);

        leaderElectionService.setOnStabilizedCallback(() -> agentStateMachine.setState(Dto.AgentState.BUILDING_SPANNING_TREE));
        spanningTreeService.setOnStabilizedCallback(() -> agentStateMachine.setState(Dto.AgentState.RAYMOND_MUTUAL_EXCLUSION));

        agentStateMachine.setState(Dto.AgentState.LEADER_ELECTION);
    }
}
