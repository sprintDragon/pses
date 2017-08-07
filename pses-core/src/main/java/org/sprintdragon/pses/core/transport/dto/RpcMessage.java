package org.sprintdragon.pses.core.transport.dto;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

@Data
@ToString
public class RpcMessage {

    private InetSocketAddress remoteAddress;

    private long timeout;

    private boolean compress;

}
