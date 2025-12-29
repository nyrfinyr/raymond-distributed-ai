package it.alesvale.node.data;


import lombok.Builder;

import java.util.UUID;

public abstract class Dto {

    public record NodeId(String nodeId) implements Comparable<NodeId> {
        public NodeId(UUID uuid){
            this(uuid.toString());
        }
        public UUID getIdAsUUID(){
            return UUID.fromString(nodeId);
        }

        @Override
        public int compareTo(NodeId o) {
            return this.getIdAsUUID().compareTo(o.getIdAsUUID());
        }
    }

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    @Builder
    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            NodeStatus status,
                            NodeId edgeTo,
                            boolean leader) {}
}
