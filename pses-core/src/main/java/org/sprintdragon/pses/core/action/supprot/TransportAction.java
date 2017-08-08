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
import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.action.ActionListener;
import org.sprintdragon.pses.core.action.ActionRequest;
import org.sprintdragon.pses.core.action.ActionResponse;
import org.sprintdragon.pses.core.action.exception.ActionRequestValidationException;
import org.sprintdragon.pses.core.cluster.metadata.IndexNameExpressionResolver;
import org.sprintdragon.pses.core.common.ParseFieldMatcher;
import org.sprintdragon.pses.core.common.component.AbstractComponent;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.threadpool.ThreadPool;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;

import static org.sprintdragon.pses.core.action.supprot.PlainActionFuture.newFuture;

/**
 * Transport*Action，两层映射关系解析
 */
@Slf4j
public abstract class TransportAction<Request extends ActionRequest, Response extends ActionResponse> extends AbstractComponent {

    @Resource
    protected ThreadPool threadPool;
    protected final String actionName;
    private ActionFilter[] filters;
    protected final ParseFieldMatcher parseFieldMatcher;
    @Resource
    protected IndexNameExpressionResolver indexNameExpressionResolver;


    protected TransportAction(Settings settings, String actionName) {
        super(settings);
        this.actionName = actionName;
        this.parseFieldMatcher = new ParseFieldMatcher();
    }

    public final ActionFuture<Response> execute(Request request) {
        PlainActionFuture<Response> future = newFuture();
        execute(request, future);
        return future;
    }

    public final void execute(Request request, ActionListener<Response> listener) {
        ActionRequestValidationException validationException = request.validate();
        if (validationException != null) {
            listener.onFailure(validationException);
            return;
        }

        if (filters.length == 0) {
            try {
                doExecute(request, listener);
            } catch (Throwable t) {
                log.trace("Error during transport action execution.", t);
                listener.onFailure(t);
            }
        } else {
            RequestFilterChain requestFilterChain = new RequestFilterChain<>(this);
            requestFilterChain.proceed(actionName, request, listener);
        }
    }

    protected abstract void doExecute(Request request, ActionListener<Response> listener);

    private static class RequestFilterChain<Request extends ActionRequest, Response extends ActionResponse> implements ActionFilterChain {

        private final TransportAction<Request, Response> action;
        private final AtomicInteger index = new AtomicInteger();

        private RequestFilterChain(TransportAction<Request, Response> action) {
            this.action = action;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void proceed(String actionName, ActionRequest request, ActionListener listener) {
            int i = index.getAndIncrement();
            try {
                if (i < this.action.filters.length) {
                    this.action.filters[i].apply(actionName, request, listener, this);
                } else if (i == this.action.filters.length) {
                    this.action.doExecute((Request) request, new FilteredActionListener<Response>(actionName, listener, new ResponseFilterChain(this.action.filters)));
                } else {
                    listener.onFailure(new IllegalStateException("proceed was called too many times"));
                }
            } catch (Throwable t) {
                log.trace("Error during transport action execution.", t);
                listener.onFailure(t);
            }
        }

        @Override
        public void proceed(String action, ActionResponse response, ActionListener listener) {
            assert false : "request filter chain should never be called on the response side";
        }
    }

    private static class ResponseFilterChain implements ActionFilterChain {

        private final ActionFilter[] filters;
        private final AtomicInteger index;

        private ResponseFilterChain(ActionFilter[] filters) {
            this.filters = filters;
            this.index = new AtomicInteger(filters.length);
        }

        @Override
        public void proceed(String action, ActionRequest request, ActionListener listener) {
            assert false : "response filter chain should never be called on the request side";
        }

        @Override
        @SuppressWarnings("unchecked")
        public void proceed(String action, ActionResponse response, ActionListener listener) {
            int i = index.decrementAndGet();
            try {
                if (i >= 0) {
                    filters[i].apply(action, response, listener, this);
                } else if (i == -1) {
                    listener.onResponse(response);
                } else {
                    listener.onFailure(new IllegalStateException("proceed was called too many times"));
                }
            } catch (Throwable t) {
                log.trace("Error during transport action execution.", t);
                listener.onFailure(t);
            }
        }
    }

    private static class FilteredActionListener<Response extends ActionResponse> implements ActionListener<Response> {

        private final String actionName;
        private final ActionListener listener;
        private final ResponseFilterChain chain;

        private FilteredActionListener(String actionName, ActionListener listener, ResponseFilterChain chain) {
            this.actionName = actionName;
            this.listener = listener;
            this.chain = chain;
        }

        @Override
        public void onResponse(Response response) {
            chain.proceed(actionName, response, listener);
        }

        @Override
        public void onFailure(Throwable e) {
            listener.onFailure(e);
        }

    }
}
