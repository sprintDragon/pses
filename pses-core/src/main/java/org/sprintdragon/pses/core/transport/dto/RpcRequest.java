package org.sprintdragon.pses.core.transport.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RpcRequest extends RpcMessage {

    //相当于requestId
    private Long requestId;

    //权限校验用
    private String appName;

    private String token;

    private String userId;

    //调用的action名
    private String actionName;

    //调用的参数
    private String paramJson;

}
