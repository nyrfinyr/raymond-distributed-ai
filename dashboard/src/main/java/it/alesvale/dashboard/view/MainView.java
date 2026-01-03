package it.alesvale.dashboard.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import it.alesvale.dashboard.backend.BrokerSubscriber;
import it.alesvale.dashboard.component.LogSidePanel;
import it.alesvale.dashboard.component.StatusLegend;
import it.alesvale.dashboard.dto.Dto;
import it.alesvale.dashboard.frontendlib.NetworkGraph;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route("")
public class MainView extends HorizontalLayout {

    private final NetworkGraph graph;
    private final BrokerSubscriber subscriber;
    private final LogSidePanel logSidePanel;
    private Registration eventRegistration;
    private SplitLayout splitLayout;
    private final ScheduledExecutorService scheduler;

    public MainView(BrokerSubscriber subscriber,
                    ObjectMapper mapper,
                    @Qualifier("eventLogs") List<Dto.NodeEvent> eventLogs){

        this.graph = new NetworkGraph(mapper);
        this.subscriber = subscriber;
        this.logSidePanel = new LogSidePanel(eventLogs);
        this.scheduler = Executors.newScheduledThreadPool(1);
        init();
    }

    private void init(){
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Div graphWrapper = new Div();
        graphWrapper.setSizeFull();
        graphWrapper.getStyle().set("position", "relative"); // Contesto per il posizionamento assoluto
        graphWrapper.getStyle().set("overflow", "hidden");   // Evita scrollbar se il grafo sborda

        StatusLegend legend = new StatusLegend();
        legend.getStyle().set("position", "absolute");
        legend.getStyle().set("top", "20px");
        legend.getStyle().set("left", "20px");
        legend.getStyle().set("z-index", "10");

        graphWrapper.add(graph, legend);

        splitLayout = new SplitLayout(graphWrapper, logSidePanel);
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        logSidePanel.setOnToggleListener(isOpen -> {
            this.graph.fit();
            if (isOpen) {
                splitLayout.setSplitterPosition(55);
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
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        var ui = attachEvent.getUI();
        scheduler.schedule(() -> {
            ui.access(graph::fit);
        }, 5, TimeUnit.SECONDS);
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
        String label = event.holder()
                ? nodeId.getHumanReadableId() + " (PRIVILEGE)"
                : nodeId.getHumanReadableId();
        Dto.NodeData node = new Dto.NodeData(nodeId, label);

        graph.addNode(node);

        if(!Objects.isNull(event.status()))
            graph.updateNodeStatus(nodeId, event.status());

        if(!Objects.isNull(event.edgeTo()))
            graph.addEdge(new Dto.EdgeData(nodeId, event.edgeTo()));
    }
}
