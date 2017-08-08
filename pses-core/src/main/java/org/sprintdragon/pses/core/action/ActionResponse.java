package org.sprintdragon.pses.core.action;

import org.sprintdragon.pses.core.transport.dto.RpcResponse;

/**
 * Created by wangdi on 17-8-8.
 */
public abstract class ActionResponse extends RpcResponse {

    public ActionResponse() {
    }

    public ActionResponse(RpcResponse message) {
        super(message);
    }
}
