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

import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.action.ActionListener;
import org.sprintdragon.pses.core.common.util.concurrent.AbstractRunnable;
import org.sprintdragon.pses.core.threadpool.ThreadPool;

import java.util.concurrent.Future;

/**
 * An action listener that wraps another action listener and threading its execution.
 */
@Slf4j
public final class ThreadedActionListener<Response> implements ActionListener<Response> {

    /**
     * Wrapper that can be used to automatically wrap a listener in a threaded listener if needed.
     */
    public static class Wrapper {

        private final ThreadPool threadPool;

        private final boolean threadedListener;

        public Wrapper(ThreadPool threadPool) {
            this.threadPool = threadPool;
            // Should the action listener be threaded or not by default. Action listeners are automatically threaded for client
            // nodes and transport client in order to make sure client side code is not executed on IO threads.
            this.threadedListener = true;
//                    = DiscoveryNode.clientNode(settings) || TransportClient.CLIENT_TYPE.equals(settings.get(Client.CLIENT_TYPE_SETTING));
        }

        public <Response> ActionListener<Response> wrap(ActionListener<Response> listener) {
            if (threadedListener == false) {
                return listener;
            }
            // if its a future, the callback is very lightweight (flipping a bit) so no need to wrap it
            if (listener instanceof Future) {
                return listener;
            }
            // already threaded...
            if (listener instanceof ThreadedActionListener) {
                return listener;
            }
            return new ThreadedActionListener<>(threadPool, ThreadPool.Names.LISTENER, listener);
        }
    }

    private final ThreadPool threadPool;
    private final String executor;
    private final ActionListener<Response> listener;

    public ThreadedActionListener(ThreadPool threadPool, String executor, ActionListener<Response> listener) {
        this.threadPool = threadPool;
        this.executor = executor;
        this.listener = listener;
    }

    @Override
    public void onResponse(final Response response) {
        threadPool.executor(executor).execute(new AbstractRunnable() {
            @Override
            protected void doRun() throws Exception {
                listener.onResponse(response);
            }

            @Override
            public void onFailure(Throwable t) {
                listener.onFailure(t);
            }
        });
    }

    @Override
    public void onFailure(final Throwable e) {
        threadPool.executor(executor).execute(new AbstractRunnable() {
            @Override
            protected void doRun() throws Exception {
                listener.onFailure(e);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("failed to execute failure callback on [{}], failure [{}]", t, listener, e);
            }
        });
    }
}
