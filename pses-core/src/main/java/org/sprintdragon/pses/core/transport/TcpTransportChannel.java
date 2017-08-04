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
package org.sprintdragon.pses.core.transport;

import io.netty.channel.Channel;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpTransportChannel implements TransportChannel {
    private final TcpTransport transport;
    protected final String action;
    protected final long requestId;
    private final String profileName;
    private final AtomicBoolean released = new AtomicBoolean();
    private final String channelType;
    private final Channel channel;

    public TcpTransportChannel(TcpTransport transport, Channel channel, String channelType, String action,
                               long requestId, String profileName) {
        this.channel = channel;
        this.transport = transport;
        this.action = action;
        this.requestId = requestId;
        this.profileName = profileName;
        this.channelType = channelType;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public String action() {
        return this.action;
    }

    @Override
    public void sendResponse(RpcResponse response) throws IOException {
        try {
            transport.sendMessage(channel, response, new Runnable() {
                @Override
                public void run() {

                }
            });
        } finally {
            release(false);
        }
    }

    @Override
    public void sendResponse(Exception exception) throws IOException {
        try {
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setError(exception);
            rpcResponse.setRequestId(requestId);
            transport.sendMessage(channel, rpcResponse, new Runnable() {
                @Override
                public void run() {

                }
            });
        } finally {
            release(true);
        }
    }

    private Exception releaseBy;

    private void release(boolean isExceptionResponse) {
        if (released.compareAndSet(false, true)) {
//            assert (releaseBy = new Exception()) != null; // easier to debug if it's already closed
//            transport.getInFlightRequestBreaker().addWithoutBreaking(-reservedBytes);
        } else if (isExceptionResponse == false) {
            // only fail if we are not sending an error - we might send the error triggered by the previous
            // sendResponse call
            throw new IllegalStateException("reserved bytes are already released", releaseBy);
        }
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public String getChannelType() {
        return channelType;
    }

    public Channel getChannel() {
        return channel;
    }

}

