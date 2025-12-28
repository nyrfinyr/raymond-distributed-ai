package it.alesvale.dashboard.view;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import it.alesvale.dashboard.backend.BrokerSubscriber;
import it.alesvale.dashboard.dto.Dto;
import it.alesvale.dashboard.frontendlib.NetworkGraph;

import java.util.Objects;

@Route("")
public class MainView extends HorizontalLayout {

    private final NetworkGraph graph;
    private final BrokerSubscriber subscriber;
    private Registration eventRegistration;

    public MainView(BrokerSubscriber subscriber){

        this.graph = new NetworkGraph();
        this.subscriber = subscriber;
        init();
    }

    private void init(){
        setSizeFull();
        setPadding(false);
        add(graph);

        eventRegistration = subscriber.getDispatcher(event ->
                getUI().ifPresent(ui -> ui.access(() -> onNodeEvent(event))));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (eventRegistration != null) {
            eventRegistration.remove();
            eventRegistration = null;
        }
        super.onDetach(detachEvent);
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

        if(!Objects.isNull(event.status()))
            graph.updateNodeStatus(nodeId, event.status());

        if(!Objects.isNull(event.edgeTo()))
            graph.addEdge(new Dto.EdgeData(nodeId, event.edgeTo().toString()));
    }

    private void shutdownEvent(Dto.NodeEvent event){

    }

    private void updateEvent(Dto.NodeEvent event){
        String nodeId = event.nodeId().toString();

        if(!Objects.isNull(event.status()))
            graph.updateNodeStatus(nodeId, event.status());

        if(!Objects.isNull(event.edgeTo()))
            graph.addEdge(new Dto.EdgeData(nodeId, event.edgeTo().toString()));
    }
}
