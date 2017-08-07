package org.sprintdragon.pses.core.transport;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-3.
 */
public interface TransportServiceAdapter extends TransportConnectionListener{

    /**
     * called by the {@link Transport) implementation when an incoming request arrives but before
     * any parsing of it has happened (with the exception of the requestId and action)
     */
    void onRequestReceived(long requestId, String action);

    /**
     * called by the {@link Transport) implementation when a response or an exception has been recieved for a previously
     * sent request (before any processing or deserialization was done). Returns the appropriate response handler or null if not
     * found.
     */
    TransportResponseHandler onResponseReceived(long requestId);

    void raiseNodeConnected(DiscoveryNode node);

    void raiseNodeDisconnected(DiscoveryNode node);

}
