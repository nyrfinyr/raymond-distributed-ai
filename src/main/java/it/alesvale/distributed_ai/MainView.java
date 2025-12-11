package it.alesvale.distributed_ai;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button; // Solo per test

import java.util.List;
import java.util.ArrayList;

@Route("")
public class MainView extends VerticalLayout {

    private final NetworkGraph graph;

    public MainView() {
        setSizeFull();
        setPadding(false);

        // 1. Crea il componente
        graph = new NetworkGraph();
        add(graph);

        // 2. Dati Mock per testare subito (dopo collegherai il backend vero)
        List<NetworkGraph.NodeData> nodes = new ArrayList<>();
        nodes.add(new NetworkGraph.NodeData("1", "Nodo 1 (Root)"));
        nodes.add(new NetworkGraph.NodeData("2", "Nodo 2"));
        nodes.add(new NetworkGraph.NodeData("3", "Nodo 3"));

        List<NetworkGraph.EdgeData> edges = new ArrayList<>();
        // 2 e 3 puntano a 1 (1 ha il token)
        edges.add(new NetworkGraph.EdgeData("2", "1"));
        edges.add(new NetworkGraph.EdgeData("3", "1"));

        // 3. Renderizza (usiamo un listener per essere sicuri che il JS sia pronto)
        addAttachListener(e -> {
            graph.setTopology(nodes, edges);
            graph.updateNodeStatus("1", NetworkGraph.NodeStatus.CRITICAL); // 1 ha il token
            graph.fit();
        });

        // 4. Bottone di Test per simulare l'algoritmo
        Button testBtn = new Button("Simula Passaggio Token (1 -> 2)", click -> {
            // 1 esce dalla CS, diventa Idle
            graph.updateNodeStatus("1", NetworkGraph.NodeStatus.IDLE);

            // L'arco si inverte: ora 1 punta a 2
            graph.reverseEdge("1", "2");

            // 2 entra in CS
            graph.updateNodeStatus("2", NetworkGraph.NodeStatus.CRITICAL);
        });

        // Metti il bottone sopra il grafo (z-index) o in un layout separato
        testBtn.getStyle().set("position", "absolute").set("top", "10px").set("left", "10px").set("z-index", "100");
        add(testBtn);
    }
}