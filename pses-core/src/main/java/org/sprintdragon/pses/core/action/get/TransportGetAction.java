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

package org.sprintdragon.pses.core.action.get;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.action.supprot.single.shard.TransportSingleShardAction;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.index.shard.ShardId;
import org.sprintdragon.pses.core.threadpool.ThreadPool;
import org.sprintdragon.pses.core.transport.TransportService;

/**
 * Performs the get operation.
 */
@Component
public class TransportGetAction extends TransportSingleShardAction<GetRequest, GetResponse> {

//    private final boolean realtime;

    public TransportGetAction(Settings settings) {
        super(settings, GetAction.NAME, GetRequest.class, ThreadPool.Names.GET);
//        this.indicesService = indicesService;

//        this.realtime = settings.getAsBoolean("action.get.realtime", true);
    }

    @Override
    protected boolean resolveIndex(GetRequest request) {
        return true;
    }

    @Override
    protected GetResponse shardOperation(GetRequest request, ShardId shardId) {
        return null;
    }

    @Override
    protected GetResponse newResponse() {
        return new GetResponse();
    }
}
