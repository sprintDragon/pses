package org.sprintdragon.pses.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * Created by wangdi on 17-8-3.
 */
@Configuration
public class ServerConfiguration {

    @Resource
    Settings settings;

    @Bean
    public NetworkService networkService() {
        return new NetworkService(settings, Collections.emptyList());
    }

}
