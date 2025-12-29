package it.alesvale.dashboard.backend;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class BrokerConfig {

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        String brokerUrl = System.getenv("BROKER_URL");
        return Nats.connect(brokerUrl);
    }
}
