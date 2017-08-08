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

package org.sprintdragon.pses.core.action.supprot;

import org.sprintdragon.pses.core.action.ActionListener;
import org.sprintdragon.pses.core.action.ActionRequest;
import org.sprintdragon.pses.core.action.ActionResponse;

/**
 * A filter chain allowing to continue and process the transport action request
 */
public interface ActionFilterChain {

    /**
     * Continue processing the request. Should only be called if a response has not been sent through
     * the given {@link ActionListener listener}
     */
    void proceed(final String action, final ActionRequest request, final ActionListener listener);

    /**
     * Continue processing the response. Should only be called if a response has not been sent through
     * the given {@link ActionListener listener}
     */
    void proceed(final String action, final ActionResponse response, final ActionListener listener);
}
