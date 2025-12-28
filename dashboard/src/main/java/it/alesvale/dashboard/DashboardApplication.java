package it.alesvale.dashboard;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Push // aggiornamenti in tempo reale
public class DashboardApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
