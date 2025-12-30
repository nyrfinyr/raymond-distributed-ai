package it.alesvale.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public abstract class Dto {

    public record NodeId(String nodeId, String swarmName)
            implements Comparable<NodeId> {

        @JsonIgnore
        public UUID getIdAsUUID(){
            return UUID.fromString(nodeId);
        }

        @Override
        public int compareTo(NodeId o) {
            return this.getIdAsUUID().compareTo(o.getIdAsUUID());
        }
    }

    public record NodeData(NodeId nodeId, String label) {}

    public record EdgeData(NodeId from, NodeId to) {}

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            NodeStatus status,
                            NodeId edgeTo,
                            boolean leader) {
    }
}
