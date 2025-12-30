package it.alesvale.node.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.UUID;

public abstract class Dto {

    public record NodeId(String nodeId, String swarmName)
            implements Comparable<NodeId> {

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

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    @Builder
    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            NodeStatus status,
                            NodeId edgeTo,
                            boolean leader) {}

}
