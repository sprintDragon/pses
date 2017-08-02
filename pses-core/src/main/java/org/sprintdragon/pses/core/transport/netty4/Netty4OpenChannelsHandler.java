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

package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.common.lease.Releasable;
import org.sprintdragon.pses.core.common.metrics.CounterMetric;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
@Slf4j
public class Netty4OpenChannelsHandler extends ChannelInboundHandlerAdapter implements Releasable {

    final Set<Channel> openChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final CounterMetric openChannelsMetric = new CounterMetric();
    final CounterMetric totalChannelsMetric = new CounterMetric();

    final ChannelFutureListener remover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            boolean removed = openChannels.remove(future.channel());
            if (removed) {
                openChannelsMetric.dec();
            }
            if (log.isTraceEnabled()) {
                log.trace("channel closed: {}", future.channel());
            }
        }
    };

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("channel opened: {}", ctx.channel());
        }
        final boolean added = openChannels.add(ctx.channel());
        if (added) {
            openChannelsMetric.inc();
            totalChannelsMetric.inc();
            ctx.channel().closeFuture().addListener(remover);
        }

        super.channelActive(ctx);
    }

    public long numberOfOpenChannels() {
        return openChannelsMetric.count();
    }

    public long totalChannels() {
        return totalChannelsMetric.count();
    }

    @Override
    public void close() {
        try {
            closeChannels(openChannels);
        } catch (IOException e) {
            log.trace("exception while closing channels", e);
        }
        openChannels.clear();
    }

    public static void closeChannels(final Collection<Channel> channels) throws IOException {
        IOException closingExceptions = null;
        final List<ChannelFuture> futures = new ArrayList<>();
        for (final Channel channel : channels) {
            try {
                if (channel != null && channel.isOpen()) {
                    futures.add(channel.close());
                }
            } catch (Exception e) {
                if (closingExceptions == null) {
                    closingExceptions = new IOException("failed to close channels");
                }
                closingExceptions.addSuppressed(e);
            }
        }
        for (final ChannelFuture future : futures) {
            future.awaitUninterruptibly();
        }

        if (closingExceptions != null) {
            throw closingExceptions;
        }
    }

}
