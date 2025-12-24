package it.alesvale.dashboard.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;
import it.alesvale.dashboard.dto.Dto;
import it.alesvale.dashboard.frontendlib.NetworkGraph;

import java.util.List;
import java.util.ArrayList;

@Route("/test")
public class TestView extends VerticalLayout {

    private final NetworkGraph graph;

    public TestView() {
        setSizeFull();
        setPadding(false);

        // 1. Crea il componente
        graph = new NetworkGraph();
        add(graph);

        // 2. Dati Mock per testing
        List<Dto.NodeData> nodes = new ArrayList<>();
        nodes.add(new Dto.NodeData("1", "Nodo 1 (Root)"));
        nodes.add(new Dto.NodeData("2", "Nodo 2"));
        nodes.add(new Dto.NodeData("3", "Nodo 3"));

        List<Dto. EdgeData> edges = new ArrayList<>();
        // 2 e 3 puntano a 1 (1 ha il token)
        edges.add(new Dto.EdgeData("2", "1"));
        edges.add(new Dto.EdgeData("3", "1"));

        // 3. Renderizza
        addAttachListener(e -> {
            graph.setTopology(nodes, edges);
            graph.updateNodeStatus("1", Dto.NodeStatus.CRITICAL); // 1 ha il token
            graph.fit();
        });

        // 4. Bottone di Test per simulare l'algoritmo
        Button testBtn = new Button("Simula Passaggio Token (1 -> 2)", click -> {
            // 1 esce dalla CS, diventa Idle
            graph.updateNodeStatus("1", Dto.NodeStatus.IDLE);

            // L'arco si inverte: ora 1 punta a 2
            graph.reverseEdge("1", "2");

            // 2 entra in CS
            graph.updateNodeStatus("2", Dto.NodeStatus.CRITICAL);
        });

        testBtn.getStyle().set("position", "absolute").set("top", "10px").set("left", "10px").set("z-index", "100");
        add(testBtn);
    }
}