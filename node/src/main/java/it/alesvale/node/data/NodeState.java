package it.alesvale.node.data;

import it.alesvale.node.Utils;
import lombok.Data;

@Data
public class NodeState {

    private final Dto.NodeId id;
    private Dto.NodeStatus status;
    private Dto.NodeId parent;
    private volatile Dto.NodeId leaderId;

    public NodeState(String swarmName) {
        this.id = new Dto.NodeId(Utils.generateNodeId(), swarmName);
        this.status = Dto.NodeStatus.IDLE;
        this.parent = null;
        this.leaderId = this.id;
    }

    public boolean isLeader(){
        return this.leaderId.nodeId().equals(this.id.nodeId());
    }
}