package it.alesvale.node;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;

@Data
public class NodeState {

    private final ObjectIdGenerators.UUIDGenerator uuidGenerator = new ObjectIdGenerators.UUIDGenerator();
    private final Dto.NodeId id = new Dto.NodeId(uuidGenerator.generateId(null));
    private Dto.NodeStatus status = Dto.NodeStatus.IDLE;
    private Dto.NodeId edgeTo = null;

}