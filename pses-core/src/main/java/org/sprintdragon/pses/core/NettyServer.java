package org.sprintdragon.pses.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.transport.TcpTransport;
import org.sprintdragon.pses.core.transport.TransportService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Created by wangdi on 17-8-2.
 */
@Component
@Slf4j
public class NettyServer implements ApplicationContextAware {

    @Resource
    TransportService transportService;
    @Resource
    TcpTransport transport;

    @PostConstruct
    public void start() throws Exception {
        transportService.start();
        transport.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
