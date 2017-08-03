package org.sprintdragon.pses.core.cluster.node;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

/**
 * Created by wangdi on 17-8-3.
 */
@Data
@ToString
public class DiscoveryNode {

    private String nodeId;
    private InetSocketAddress address;

    public String id() {
        return nodeId;
    }

    public InetSocketAddress address() {
        return address;
    }
}
