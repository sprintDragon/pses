package org.sprintdragon.pses.core.cluster.service;

import org.springframework.stereotype.Service;
import org.sprintdragon.pses.core.cluster.ClusterService;
import org.sprintdragon.pses.core.common.component.Lifecycle;
import org.sprintdragon.pses.core.common.component.LifecycleListener;

/**
 * Created by wangdi on 17-8-8.
 */
@Service
public class InternalClusterService implements ClusterService {
    @Override
    public void close() {

    }

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() {

    }
}
