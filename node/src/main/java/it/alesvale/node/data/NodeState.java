package it.alesvale.node.data;

import it.alesvale.node.Utils;
import lombok.Data;

@Data
public class NodeState {

    private final Dto.NodeId id = new Dto.NodeId(Utils.generateNodeId());
    private Dto.NodeStatus status = Dto.NodeStatus.IDLE;
    private Dto.NodeId edgeTo = null;

    //Leader election
    private volatile Dto.NodeId leaderId = this.id;

    public boolean isLeader(){
        return this.leaderId.nodeId().equals(this.id.nodeId());
    }
}