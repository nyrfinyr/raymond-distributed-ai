package it.alesvale.dashboard.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class StatusLegend extends Div {

    public StatusLegend() {
        getStyle().set("background-color", "rgba(255, 255, 255, 0.95)"); // Bianco quasi opaco
        getStyle().set("padding", "15px");
        getStyle().set("border-radius", "8px");
        getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)"); // Ombra per dare profondità
        getStyle().set("border", "1px solid #e0e0e0");
        getStyle().set("width", "fit-content");
        getStyle().set("min-width", "160px");
        getStyle().set("pointer-events", "none"); // Permette di cliccare attraverso il box (opzionale, rimuovere se si vuole selezionare il testo)

        Span title = new Span("Stato Nodi");
        title.getStyle().set("font-weight", "bold");
        title.getStyle().set("font-size", "0.9em");
        title.getStyle().set("color", "#333");
        title.getStyle().set("display", "block");
        title.getStyle().set("margin-bottom", "10px");
        title.getStyle().set("border-bottom", "1px solid #eee");
        title.getStyle().set("padding-bottom", "5px");

        add(title);

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.setMargin(false);

        list.add(createLegendItem("#97C2FC", "IDLE (Inattivo)"));
        list.add(createLegendItem("#FFD700", "REQUESTING (In attesa)"));
        list.add(createLegendItem("#FF4500", "CRITICAL (Sezione Critica)"));

        add(list);
    }

    private HorizontalLayout createLegendItem(String colorCode, String labelText) {
        HorizontalLayout item = new HorizontalLayout();
        item.setAlignItems(HorizontalLayout.Alignment.CENTER);
        item.setSpacing(true);
        item.getStyle().set("margin-bottom", "6px");

        Div dot = new Div();
        dot.setWidth("12px");
        dot.setHeight("12px");
        dot.getStyle().set("background-color", colorCode);
        dot.getStyle().set("border-radius", "50%");
        dot.getStyle().set("border", "1px solid rgba(0,0,0,0.1)");
        dot.getStyle().set("flex-shrink", "0");

        Span label = new Span(labelText);
        label.getStyle().set("font-size", "0.85em");
        label.getStyle().set("color", "#555");

        item.add(dot, label);
        return item;
    }
}
