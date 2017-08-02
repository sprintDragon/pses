package org.sprintdragon.pses.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.netty4.Netty4Transport;

import java.util.Collections;

/**
 * Created by patterncat on 2016-04-08.
 */
@SpringBootApplication
public class PsesServerApplication {

    @Bean
    public Settings settings() {
        return new Settings();
    }

    @Bean
    public NetworkService networkService() {
        return new NetworkService(settings(), Collections.emptyList());
    }

    @Bean
    public Netty4Transport netty4Transport() {
        return new Netty4Transport(settings(), networkService());
    }

    @Bean
    public TransportService transportService() {
        return new TransportService(settings(), netty4Transport());
    }


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PsesServerApplication.class);
        app.setWebEnvironment(false);
        app.run(args);
    }
}
