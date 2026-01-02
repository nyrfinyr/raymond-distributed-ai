package it.alesvale.dashboard.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import it.alesvale.dashboard.dto.Dto;
import lombok.Setter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class LogSidePanel extends VerticalLayout {

    private final List<Dto.NodeEvent> eventLog;
    private final ListDataProvider<Dto.NodeEvent> dataProvider;
    private final Grid<Dto.NodeEvent> eventGrid;
    private final TextField searchField;
    private final Button toggleButton;
    private boolean isPanelOpen = true;
    @Setter
    private Consumer<Boolean> onToggleListener;

    public LogSidePanel(List<Dto.NodeEvent> eventLog) {
        this.eventLog = eventLog;

        setHeightFull();
        setPadding(false);
        setSpacing(false);

        // Inizializzazione dati e griglia
        this.eventGrid = new Grid<>();
        this.dataProvider = new ListDataProvider<>(eventLog);
        eventGrid.setDataProvider(dataProvider);

        eventGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_NO_BORDER
        );

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        eventGrid.addColumn(event -> event.nodeId().nodeId())
                .setHeader("NodeId").setResizable(true).setWidth("20%").setFlexGrow(2);
        eventGrid.addColumn(event -> event.timestamp().atZone(ZoneId.systemDefault()).format(dateFormatter))
                .setHeader("Timestamp").setResizable(true).setWidth("20%").setFlexGrow(2);
        eventGrid.addColumn(Dto.NodeEvent::message)
                .setHeader("Message").setResizable(true).setWidth("60%").setFlexGrow(6);
        eventGrid.setSizeFull();

        // Configurazione Search Field
        searchField = new TextField();
        searchField.setPlaceholder("Filtra per nodo");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> {
            dataProvider.setFilter(event -> {
                String filterText = e.getValue();
                if (filterText == null || filterText.isEmpty()) return true;
                return event.nodeId().nodeId().toLowerCase().contains(filterText.toLowerCase());
            });
        });

        // Configurazione Toggle Button
        toggleButton = new Button(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
        toggleButton.addClickListener(e -> toggleSidePanel());

        HorizontalLayout header = new HorizontalLayout(toggleButton, searchField);
        header.setWidthFull();
        header.setPadding(true);
        header.setAlignItems(Alignment.CENTER);
        header.setFlexGrow(1, searchField);

        add(header, eventGrid);

        openSidePanel();
    }

    public void addEvent(Dto.NodeEvent event) {
        eventLog.add(0, event);
        
        if (eventLog.size() > 1000) {
            eventLog.remove(eventLog.size() - 1);
        }
        
        dataProvider.refreshAll();
    }

    private void toggleSidePanel() {
        if (isPanelOpen) {
            closeSidePanel();
        } else {
            openSidePanel();
        }

        if (onToggleListener != null) {
            onToggleListener.accept(isPanelOpen);
        }
    }

    public void setSearchFilter(String nodeId) {
        searchField.setValue(nodeId != null ? nodeId : "");

        if (!isPanelOpen) {
            openSidePanel();
            if (onToggleListener != null) {
                onToggleListener.accept(true);
            }
        }
    }

    private void closeSidePanel() {
        isPanelOpen = false;
        searchField.setVisible(false);
        eventGrid.setVisible(false);
        
        toggleButton.setIcon(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
    }

    private void openSidePanel() {
        isPanelOpen = true;
        searchField.setVisible(true);
        eventGrid.setVisible(true);
        
        toggleButton.setIcon(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
    }
}
