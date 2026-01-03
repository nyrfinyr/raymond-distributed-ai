package it.alesvale.node.service;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class DashboardUpdateUtils {

    public static void sendAliveEvent(Broker broker, NodeState state) {
        try {
            Dto.NodeId edgeTarget = state.getParent();

            if (state.getHolder() != null) {
                edgeTarget = state.getHolder();
            }

            if (edgeTarget != null && edgeTarget.equals(state.getId())) {
                edgeTarget = null;
            }

            Dto.NodeEvent aliveEvent = Dto.NodeEvent.builder()
                    .nodeId(state.getId())
                    .eventType(Dto.NodeEventType.I_AM_ALIVE)
                    .status(state.getStatus())
                    .edgeTo(edgeTarget)
                    .holder(state.isHolder())
                    .build();

            broker.publishEvent(aliveEvent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
