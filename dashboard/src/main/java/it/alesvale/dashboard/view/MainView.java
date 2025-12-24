package it.alesvale.dashboard.view;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import it.alesvale.dashboard.backend.BrokerSubscriber;
import it.alesvale.dashboard.dto.Dto;
import it.alesvale.dashboard.frontendlib.NetworkGraph;

@Route("")
public class MainView extends HorizontalLayout {

    private final NetworkGraph graph;
    private final BrokerSubscriber subscriber;

    public MainView(BrokerSubscriber subscriber){

        this.graph = new NetworkGraph();
        this.subscriber = subscriber;
        init();
    }

    private void init(){
        setSizeFull();
        setPadding(false);
        add(graph);

        subscriber.getDispatcher(this::onNodeEvent);
    }

    private void onNodeEvent(Dto.NodeEvent event){
        switch(event.eventType()) {
            case I_AM_ALIVE -> aliveEvent(event);
            case SHUTTING_DOWN -> shutdownEvent(event);
            case NODE_UPDATE -> updateEvent(event);
        }
    }

    private void aliveEvent(Dto.NodeEvent event){

        String nodeId = event.nodeId().toString();

        Dto.NodeData node = new Dto.NodeData(nodeId, nodeId);
        graph.addNode(node);

        event.status().ifPresent(status ->
                graph.updateNodeStatus(nodeId, status));

        event.edgeTo().ifPresent(edgeTo ->
                graph.addEdge(new Dto.EdgeData(nodeId, edgeTo.toString())));
    }

    private void shutdownEvent(Dto.NodeEvent event){

    }

    private void updateEvent(Dto.NodeEvent event){
        String nodeId = event.nodeId().toString();

        event.status().ifPresent(status ->
                graph.updateNodeStatus(nodeId, status));

        event.edgeTo().ifPresent(edgeTo ->
                graph.addEdge(new Dto.EdgeData(nodeId, edgeTo.toString())));
    }
}
