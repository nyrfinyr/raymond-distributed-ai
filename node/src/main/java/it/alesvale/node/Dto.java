package it.alesvale.node;

import java.util.Optional;

abstract class Dto {

    public record NodeId(String nodeId) {}

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            Optional<NodeStatus> status,
                            Optional<NodeId> edgeTo) {

        public NodeEvent(String nodeId, NodeEventType eventType){
            this(new NodeId(nodeId), eventType, Optional.empty(), Optional.empty());
        }

        public NodeEvent(String nodeId, NodeEventType eventType, NodeStatus status){
            this(new NodeId(nodeId), eventType, Optional.of(status), Optional.empty());
        }

        public NodeEvent(String nodeId, NodeEventType eventType, String edgeTo){
            this(new NodeId(nodeId), eventType, Optional.empty(), Optional.of(new NodeId(edgeTo)));
        }

        public NodeEvent(String nodeId, NodeEventType eventType, NodeStatus status, String edgeTo){
            this(new NodeId(nodeId), eventType, Optional.of(status), Optional.of(new NodeId(edgeTo)));
        }
    }
}
