package org.sprintdragon.pses.core.transport;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-3.
 */
public interface TransportServiceAdapter {

    void raiseNodeConnected(DiscoveryNode node);

    void raiseNodeDisconnected(DiscoveryNode node);

}
