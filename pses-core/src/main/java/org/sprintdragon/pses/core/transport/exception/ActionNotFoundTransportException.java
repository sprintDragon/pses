package org.sprintdragon.pses.core.transport.exception;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-8.
 */
public class ActionNotFoundTransportException extends TransportException {
    public ActionNotFoundTransportException(DiscoveryNode node) {
        super(node);
    }

    public ActionNotFoundTransportException(DiscoveryNode node, String message) {
        super(node, message);
    }

    public ActionNotFoundTransportException(DiscoveryNode node, String message, Throwable cause) {
        super(node, message, cause);
    }

    public ActionNotFoundTransportException(DiscoveryNode node, Throwable cause) {
        super(node, cause);
    }
}
