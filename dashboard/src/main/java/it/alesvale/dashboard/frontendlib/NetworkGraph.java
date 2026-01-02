package it.alesvale.dashboard.frontendlib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import it.alesvale.dashboard.dto.Dto;

import java.util.function.Consumer;

@Tag("div")
@NpmPackage(value = "vis-network", version = "9.1.9")
@NpmPackage(value = "vis-data", version = "7.1.9")
@JsModule("./js/network-connector.js")
public class NetworkGraph extends Component implements HasSize {

    private final ObjectMapper mapper;
    private Consumer<String> nodeClickListener;

    public NetworkGraph(ObjectMapper mapper) {
        this.setId("raymond-graph");
        this.setSizeFull();
        this.mapper = mapper;
    }

    public void addNodeClickListener(Consumer<String> listener) {
        this.nodeClickListener = listener;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent){
        super.onAttach(attachEvent);
        getElement().executeJs("window.initNetworkGraph($0)", getElement());
    }

    @ClientCallable
    public void onNodeClicked(String nodeId) {
        if (nodeClickListener != null) {
            nodeClickListener.accept(nodeId);
        }
    }

    /**
     * Add Node to existing graph
     */
    public void addNode(Dto.NodeData node) {
        try {
            String nodeJson = mapper.writeValueAsString(node);
            getElement().executeJs("this.addNode($0)", nodeJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add edge to existing graph
     */
    public void addEdge(Dto.EdgeData edge) {
        try {
            String edgeJson = mapper.writeValueAsString(edge);
            getElement().executeJs("this.addEdge($0)", edgeJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update node status
     */
    public void updateNodeStatus(Dto.NodeId nodeId, Dto.NodeStatus status) {
        String color = switch (status) {
            case IDLE -> "#97C2FC";        // Blu chiaro
            case REQUESTING -> "#FFD700";  // Oro/Giallo
            case CRITICAL -> "#FF4500";    // Rosso
        };
        getElement().executeJs("this.updateNodeColor($0, $1)", nodeId.nodeId(), color);
    }

    public void reverseEdge(String newHolderFrom, String newHolderTo) {
        getElement().executeJs("this.updateEdgeDirection($0, $1)", newHolderFrom, newHolderTo);
    }

}
