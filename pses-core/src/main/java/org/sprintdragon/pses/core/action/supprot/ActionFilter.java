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
import org.sprintdragon.pses.core.common.component.AbstractComponent;
import org.sprintdragon.pses.core.common.settings.Settings;

/**
 * A filter allowing to filter transport actions
 */
public interface ActionFilter {

    /**
     * The position of the filter in the chain. Execution is done from lowest order to highest.
     */
    int order();

    /**
     * Enables filtering the execution of an action on the request side, either by sending a response through the
     * {@link ActionListener} or by continuing the execution through the given {@link ActionFilterChain chain}
     */
    void apply(String action, ActionRequest request, ActionListener listener, ActionFilterChain chain);

    /**
     * Enables filtering the execution of an action on the response side, either by sending a response through the
     * {@link ActionListener} or by continuing the execution through the given {@link ActionFilterChain chain}
     */
    void apply(String action, ActionResponse response, ActionListener listener, ActionFilterChain chain);

    /**
     * A simple base class for injectable action filters that spares the implementation from handling the
     * filter chain. This base class should serve any action filter implementations that doesn't require
     * to apply async filtering logic.
     */
    public static abstract class Simple extends AbstractComponent implements ActionFilter {

        public Simple(Settings settings) {
            super(settings);
        }

        @Override
        public final void apply(String action, ActionRequest request, ActionListener listener, ActionFilterChain chain) {
            if (apply(action, request, listener)) {
                chain.proceed(action, request, listener);
            }
        }

        /**
         * Applies this filter and returns {@code true} if the execution chain should proceed, or {@code false}
         * if it should be aborted since the filter already handled the request and called the given listener.
         */
        protected abstract boolean apply(String action, ActionRequest request, ActionListener listener);

        @Override
        public final void apply(String action, ActionResponse response, ActionListener listener, ActionFilterChain chain) {
            if (apply(action, response, listener)) {
                chain.proceed(action, response, listener);
            }
        }

        /**
         * Applies this filter and returns {@code true} if the execution chain should proceed, or {@code false}
         * if it should be aborted since the filter already handled the response by calling the given listener.
         */
        protected abstract boolean apply(String action, ActionResponse response, ActionListener listener);
    }
}
