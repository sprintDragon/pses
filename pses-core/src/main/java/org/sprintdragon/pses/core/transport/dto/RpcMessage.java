package org.sprintdragon.pses.core.transport.dto;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

@Data
@ToString
public class RpcMessage<TM extends RpcMessage<TM>> {

    private InetSocketAddress remoteAddress;

    private long timeout;

    private boolean compress;

    protected RpcMessage() {
    }

    protected RpcMessage(TM message) {
        this.remoteAddress = message.getRemoteAddress();
        this.timeout = message.getTimeout();
        this.compress = message.isCompress();
    }
}
