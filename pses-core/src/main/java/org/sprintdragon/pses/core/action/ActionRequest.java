/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.sprintdragon.pses.core.action;

import org.sprintdragon.pses.core.action.exception.ActionRequestValidationException;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;

/**
 *
 */
public abstract class ActionRequest<T extends ActionRequest> extends RpcRequest {

    protected ActionRequest() {
        super();
    }

    protected ActionRequest(RpcRequest message) {
        super(message);
    }

    public abstract ActionRequestValidationException validate();

}
