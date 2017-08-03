package org.sprintdragon.pses.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sprintdragon.pses.core.common.network.NetworkService;

import java.util.Collections;

/**
 * Created by wangdi on 17-8-3.
 */
@Configuration
public class ServerConfiguration {


    @Bean
    public NetworkService networkService() {
        return new NetworkService(Collections.emptyList());
    }


}
