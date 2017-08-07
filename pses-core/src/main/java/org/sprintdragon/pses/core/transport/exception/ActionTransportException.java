package org.sprintdragon.pses.core.transport.exception;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-7.
 */
public class ActionTransportException extends TransportException {

    public ActionTransportException(DiscoveryNode node) {
        super(node);
    }

    public ActionTransportException(DiscoveryNode node, String message) {
        super(node, message);
    }

    public ActionTransportException(DiscoveryNode node, String message, Throwable cause) {
        super(node, message, cause);
    }

    public ActionTransportException(DiscoveryNode node, Throwable cause) {
        super(node, cause);
    }
}
