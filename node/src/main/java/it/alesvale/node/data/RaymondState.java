package it.alesvale.node.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Queue;

@Data
@AllArgsConstructor
public class RaymondState {
    private Dto.NodeId holder;                  // Indicates the relative position of the privileged node with respect to this node
    private boolean using;                      // Indicates if this node is currently executing the critical section
    private Queue<Dto.NodeId> requestQueue;     // Neighbors that have sent a request message, but have not yet been sent the privilege in reply
    private boolean asked;                      // True if this node has already sent a request message to current holder
}
