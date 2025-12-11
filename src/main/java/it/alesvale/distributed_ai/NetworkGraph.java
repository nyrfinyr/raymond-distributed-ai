package it.alesvale.distributed_ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import java.util.List;

@Tag("div") // Il componente sarà renderizzato come un <div> HTML
// Scarica le librerie npm necessarie (Vaadin 24 lo fa in automatico al build)
@NpmPackage(value = "vis-network", version = "9.1.9")
@NpmPackage(value = "vis-data", version = "7.1.9")
// Collega il file JS creato al punto 2
@JsModule("./js/network-connector.js")
public class NetworkGraph extends Component implements HasSize {

    private final ObjectMapper mapper = new ObjectMapper();

    public NetworkGraph() {
        this.setId("raymond-graph");
        this.setSizeFull(); // Occupa tutto lo spazio del genitore

        // Inizializza il grafo lato client appena il componente è attaccato
        // $0 rappresenta 'this' (l'elemento DOM)
        getElement().executeJs("window.initNetworkGraph($0)", getElement());
    }

    /**
     * Carica la topologia iniziale.
     * Usa classi DTO semplici (NodeData, EdgeData) per la serializzazione.
     */
    public void setTopology(List<NodeData> nodes, List<EdgeData> edges) {
        try {
            String nodesJson = mapper.writeValueAsString(nodes);
            String edgesJson = mapper.writeValueAsString(edges);
            getElement().executeJs("this.setGraphData($0, $1)", nodesJson, edgesJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void updateNodeStatus(String nodeId, NodeStatus status) {
        String color = switch (status) {
            case IDLE -> "#97C2FC";      // Blu chiaro
            case REQUESTING -> "#FFD700"; // Oro/Giallo
            case CRITICAL -> "#FF4500";   // Rosso
        };
        getElement().executeJs("this.updateNodeColor($0, $1)", nodeId, color);
    }

    public void reverseEdge(String newHolderFrom, String newHolderTo) {
        getElement().executeJs("this.updateEdgeDirection($0, $1)", newHolderFrom, newHolderTo);
    }

    /**
     * Adatta lo zoom per mostrare tutti i nodi.
     */
    public void fit() {
        getElement().executeJs("this.fitGraph()");
    }

    // --- DTO Interni o esterni per comodità ---
    public record NodeData(String id, String label) {}
    public record EdgeData(String from, String to) {}
    public enum NodeStatus { IDLE, REQUESTING, CRITICAL }
}
