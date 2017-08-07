package org.sprintdragon.pses.core.transport.exception;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-7.
 */
public class ReceiveTimeoutTransportException extends ActionTransportException {

    public ReceiveTimeoutTransportException(DiscoveryNode node) {
        super(node);
    }

    public ReceiveTimeoutTransportException(DiscoveryNode node, String message) {
        super(node, message);
    }

    public ReceiveTimeoutTransportException(DiscoveryNode node, String message, Throwable cause) {
        super(node, message, cause);
    }

    public ReceiveTimeoutTransportException(DiscoveryNode node, Throwable cause) {
        super(node, cause);
    }
}
