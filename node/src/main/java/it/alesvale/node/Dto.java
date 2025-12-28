package it.alesvale.node;


import lombok.Builder;

import java.util.UUID;

abstract class Dto {

    public record NodeId(String nodeId) {
        public NodeId(UUID uuid){
            this(uuid.toString());
        }
    }

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    @Builder
    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            NodeStatus status,
                            NodeId edgeTo) {}
}
