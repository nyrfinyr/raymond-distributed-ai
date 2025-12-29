package it.alesvale.dashboard.dto;

public abstract class Dto {

    public record NodeId(String nodeId) {}

    public record NodeData(NodeId nodeId, String label) {
        public NodeData(String nodeId, String label){
            this(new NodeId(nodeId), label);
        }
    }

    public record EdgeData(NodeId from, NodeId to) {
        public EdgeData(String from, String to){
            this(new NodeId(from), new NodeId(to));
        }
    }

    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }

    public enum NodeEventType { I_AM_ALIVE, SHUTTING_DOWN, NODE_UPDATE }

    public record NodeEvent(NodeId nodeId,
                            NodeEventType eventType,
                            NodeStatus status,
                            NodeId edgeTo,
                            boolean leader) {
    }
}
