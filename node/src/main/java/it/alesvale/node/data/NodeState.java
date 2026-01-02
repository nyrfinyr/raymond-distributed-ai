package it.alesvale.node.data;

import it.alesvale.node.Utils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class NodeState {

    private final Dto.NodeId id;
    private Dto.NodeStatus status;
    private Dto.NodeId parent;
    private Map<String, Dto.SocketAddress> childrenMap;
    private volatile Dto.NodeId leaderId;

    public NodeState(Dto.SocketAddress socketAddress) {
        this.id = new Dto.NodeId(Utils.generateNodeId(), socketAddress);
        this.status = Dto.NodeStatus.IDLE;
        this.parent = null;
        this.childrenMap = new HashMap<>();
        this.leaderId = this.id;
    }

    public void addChildren(Dto.NodeId nodeId){
        childrenMap.put(nodeId.nodeId(), nodeId.address());
    }

    public Dto.SocketAddress getChildrenAddress(Dto.NodeId nodeId){
        return childrenMap.get(nodeId.nodeId());
    }

    public boolean isLeader(){
        return this.leaderId.nodeId().equals(this.id.nodeId());
    }
}