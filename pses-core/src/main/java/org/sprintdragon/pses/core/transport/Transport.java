package org.sprintdragon.pses.core.transport;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.CheckedBiConsumer;
import org.sprintdragon.pses.core.common.component.LifecycleComponent;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangdi on 17-8-3.
 */
public interface Transport<Channel> extends LifecycleComponent {


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
    void connectToNode(DiscoveryNode node, CheckedBiConsumer<Connection, IOException> connectionValidator) throws Exception;

    /**
     * Disconnected from the given node, if not connected, will do nothing.
     */
    void disconnectFromNode(DiscoveryNode node) throws Exception;

    /**
     * Returns count of currently open connections
     */
    long serverOpen();

    long newRequestId();

    Connection openConnection(DiscoveryNode node) throws IOException;

    Connection getConnection(DiscoveryNode node);

    /**
     * A unidirectional connection to a {@link DiscoveryNode}
     */
    interface Connection extends Closeable {
        /**
         * The node this connection is associated with
         */
        DiscoveryNode getNode();

        void sendRequest(RpcRequest request) throws
                Exception;

    }
}
