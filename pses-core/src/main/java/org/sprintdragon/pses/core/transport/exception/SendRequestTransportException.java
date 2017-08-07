package org.sprintdragon.pses.core.transport.exception;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-7.
 */
public class SendRequestTransportException extends ActionTransportException  {

    public SendRequestTransportException(DiscoveryNode node) {
        super(node);
    }

    public SendRequestTransportException(DiscoveryNode node, String message) {
        super(node, message);
    }

    public SendRequestTransportException(DiscoveryNode node, String message, Throwable cause) {
        super(node, message, cause);
    }

    public SendRequestTransportException(DiscoveryNode node, Throwable cause) {
        super(node, cause);
    }
}
