package it.alesvale.node.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public abstract class Dto {

    public record SocketAddress(String hostname, int port) implements Serializable{}

    public record NodeId(String nodeId, SocketAddress address)
            implements Comparable<NodeId>, Serializable {

        @JsonIgnore
        public UUID getIdAsUUID(){
            return UUID.fromString(nodeId);
        }

        @JsonIgnore
        public String getHumanReadableId() {
            return nodeId.substring(0, 4);
        }

        @Override
        public int compareTo(NodeId o) {
            return this.getIdAsUUID().compareTo(o.getIdAsUUID());
        }
    }

    public enum NodeStatus {IDLE, REQUESTING, CRITICAL}
    public enum NodeEventType {I_AM_ALIVE, NODE_INFO}

    @Builder
    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            String message,
                            Instant timestamp,
                            NodeStatus status,
                            NodeId edgeTo,
                            boolean holder) {}

    public enum RaymondEventType {REQUEST, PRIVILEGE}
    @Builder
    public record RaymondEvent(NodeId nodeId, RaymondEventType eventType)
            implements Serializable {
        @Override
        public String toString(){
            return String.format("%s-%s", nodeId.nodeId, eventType);
        }
    }

    public enum AgentState {LEADER_ELECTION, BUILDING_SPANNING_TREE, RAYMOND_MUTUAL_EXCLUSION}
}
