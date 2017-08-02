package org.sprintdragon.pses.core.transport.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RpcResponse {

    private Long requestId;

    private Throwable error;

    //不采用java体系的话，依赖status及msg
//    private int status;

//    private String msg;

    private Object result;

}
