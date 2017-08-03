package org.sprintdragon.pses.core.transport;

import io.netty.channel.Channel;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.LifecycleComponent;

/**
 * Created by wangdi on 17-8-3.
 */
public interface Transport extends LifecycleComponent {


    void setTransportServiceAdapter(TransportServiceAdapter transportServiceAdapter);

    /**
     * The address the transport is bound on.
     */
    BoundTransportAddress boundAddress();

    /**
     * Returns <tt>true</tt> if the node is connected.
     */
    boolean nodeConnected(DiscoveryNode node);

    /**
     * Connects to the given node, if already connected, does nothing.
     */
    void connectToNode(DiscoveryNode node) throws Exception;

    /**
     * Disconnected from the given node, if not connected, will do nothing.
     */
    void disconnectFromNode(DiscoveryNode node);

    /**
     * Returns count of currently open connections
     */
    long serverOpen();

    Channel nodeChannel(DiscoveryNode node) throws Exception;
}
