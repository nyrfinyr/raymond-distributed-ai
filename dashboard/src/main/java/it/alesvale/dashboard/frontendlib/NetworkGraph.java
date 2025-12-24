package it.alesvale.dashboard.frontendlib;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import it.alesvale.dashboard.dto.Dto;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Tag("div")
@NpmPackage(value = "vis-network", version = "9.1.9")
@NpmPackage(value = "vis-data", version = "7.1.9")
@JsModule("./js/network-connector.js")
public class NetworkGraph extends Component implements HasSize {

    private final ObjectMapper mapper = new ObjectMapper();

    public NetworkGraph() {
        this.setId("raymond-graph");
        this.setSizeFull();
        getElement().executeJs("window.initNetworkGraph($0)", getElement());
    }

    /**
     * Load graph topology
     */
    public void setTopology(List<Dto.NodeData> nodes, List<Dto.EdgeData> edges) {
        try {
            String nodesJson = mapper.writeValueAsString(nodes);
            String edgesJson = mapper.writeValueAsString(edges);
            getElement().executeJs("this.setGraphData($0, $1)", nodesJson, edgesJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    public void updateNodeStatus(String nodeId, Dto.NodeStatus status) {
        String color = switch (status) {
            case IDLE -> "#97C2FC";        // Blu chiaro
            case REQUESTING -> "#FFD700";  // Oro/Giallo
            case CRITICAL -> "#FF4500";    // Rosso
        };
        getElement().executeJs("this.updateNodeColor($0, $1)", nodeId, color);
    }

    public void reverseEdge(String newHolderFrom, String newHolderTo) {
        getElement().executeJs("this.updateEdgeDirection($0, $1)", newHolderFrom, newHolderTo);
    }

    public void fit() {
        getElement().executeJs("this.fitGraph()");
    }
}
