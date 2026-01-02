package it.alesvale.dashboard.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import it.alesvale.dashboard.backend.BrokerSubscriber;
import it.alesvale.dashboard.component.LogSidePanel;
import it.alesvale.dashboard.dto.Dto;
import it.alesvale.dashboard.frontendlib.NetworkGraph;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Objects;

@Route("")
public class MainView extends HorizontalLayout {

    private final NetworkGraph graph;
    private final BrokerSubscriber subscriber;
    private final LogSidePanel logSidePanel;
    private Registration eventRegistration;
    private SplitLayout splitLayout;

    public MainView(BrokerSubscriber subscriber,
                    ObjectMapper mapper,
                    @Qualifier("eventLogs") List<Dto.NodeEvent> eventLogs){

        this.graph = new NetworkGraph(mapper);
        this.subscriber = subscriber;
        this.logSidePanel = new LogSidePanel(eventLogs);
        init();
    }

    private void init(){
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        splitLayout = new SplitLayout(graph, logSidePanel);
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(70);

        logSidePanel.setOnToggleListener(isOpen -> {
            if (isOpen) {
                splitLayout.setSplitterPosition(70);
            } else {
                splitLayout.setSplitterPosition(97);
            }
        });

        graph.addNodeClickListener(nodeId -> {
            getUI().ifPresent(ui -> ui.access(() -> logSidePanel.setSearchFilter(nodeId)));
        });

        add(splitLayout);

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
            case NODE_INFO -> infoEvent(event);
        }
    }

    private void infoEvent(Dto.NodeEvent event){
        logSidePanel.addEvent(event);
    }

    private void aliveEvent(Dto.NodeEvent event){

        Dto.NodeId nodeId = event.nodeId();
        String label = nodeId.nodeId().subSequence(0, 4).toString();
        label = event.leader() ? label + " (Root)" : label;

        Dto.NodeData node = new Dto.NodeData(nodeId, label);

        graph.addNode(node);

        if(!Objects.isNull(event.status()))
            graph.updateNodeStatus(nodeId, event.status());

        if(!Objects.isNull(event.edgeTo()))
            graph.addEdge(new Dto.EdgeData(nodeId, event.edgeTo()));
    }
}
