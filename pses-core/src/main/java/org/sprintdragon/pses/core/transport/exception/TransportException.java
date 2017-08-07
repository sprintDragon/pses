package org.sprintdragon.pses.core.transport.exception;

import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;

/**
 * Created by wangdi on 17-8-4.
 */
public class TransportException extends Exception {

    private DiscoveryNode node;

    public TransportException(DiscoveryNode node) {
        this.node = node;
    }

    public TransportException(DiscoveryNode node, String message) {
        super(message);
        this.node = node;
    }

    public TransportException(DiscoveryNode node, String message, Throwable cause) {
        super(message, cause);
        this.node = node;
    }

    public TransportException(DiscoveryNode node, Throwable cause) {
        super(cause);
        this.node = node;
    }
}
