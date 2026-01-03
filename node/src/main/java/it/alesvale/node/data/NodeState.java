package it.alesvale.node.data;

import it.alesvale.node.Utils;
import lombok.Data;

import java.util.*;

@Data
public class NodeState {

    private final Dto.NodeId id;
    private Dto.NodeStatus status;
    private Dto.NodeId parent;
    private volatile Dto.NodeId leaderId;
    private RaymondState raymondState;

    public NodeState(Dto.SocketAddress socketAddress) {
        this.id = new Dto.NodeId(Utils.generateNodeId(), socketAddress);
        this.status = Dto.NodeStatus.IDLE;
        this.parent = null;
        this.leaderId = this.id;
        this.raymondState = new RaymondState(null, false, new LinkedList<>(), false);
    }

    public String getHumanReadableId(){
        return this.id.getHumanReadableId();
    }

    /* ########################################################################################## */
    /* ########################################################################################## */

    //Holder
    public Optional<Dto.SocketAddress> getHolderAddress(){
        return Optional.ofNullable(this.raymondState.getHolder())
                .map(Dto.NodeId::address);
    }

    public void setHolder(Dto.NodeId nodeId){
        this.setParent(nodeId);
        this.raymondState.setHolder(nodeId);
    }

    public Dto.NodeId getHolder(){
        return this.raymondState.getHolder();
    }

    public boolean isHolder(){
        return this.id.equals(this.raymondState.getHolder());
    }

    /* ########################################################################################## */
    /* ########################################################################################## */

    //Using
    public void setUsing(boolean using){
        this.raymondState.setUsing(using);
    }

    public boolean isUsing(){
        return this.raymondState.isUsing();
    }

    /* ########################################################################################## */
    /* ########################################################################################## */

    //Request queue
    public boolean isQueueEmpty(){
        return this.raymondState.getRequestQueue().isEmpty();
    }
    public Dto.NodeId dequeueRequest(){
        return this.raymondState.getRequestQueue().remove();
    }
    public void enqueueRequest(Dto.NodeId nodeId){
        this.raymondState.getRequestQueue().add(nodeId);
    }

    /* ########################################################################################## */
    /* ########################################################################################## */

    //Asked
    public boolean hasAlreadyAsked(){
        return this.raymondState.isAsked();
    }
    public void setAsked(boolean asked){
        this.raymondState.setAsked(asked);
    }

    /* ########################################################################################## */
    /* ########################################################################################## */

    public boolean isLeader(){
        return this.leaderId.nodeId().equals(this.id.nodeId());
    }
}