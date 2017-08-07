package org.sprintdragon.pses.core.transport.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RpcMessage {

    private long timeout;

    private boolean compress;

}
