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

package org.sprintdragon.pses.core.action.supprot.single.shard;

import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.action.ActionListener;
import org.sprintdragon.pses.core.action.ActionResponse;
import org.sprintdragon.pses.core.action.supprot.TransportAction;
import org.sprintdragon.pses.core.cluster.ClusterService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.index.shard.ShardId;
import org.sprintdragon.pses.core.transport.TransportChannel;
import org.sprintdragon.pses.core.transport.TransportRequestHandler;
import org.sprintdragon.pses.core.transport.TransportService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * A base class for operations that need to perform a read operation on a single shard copy. If the operation fails,
 * the read operation can be performed on other shard copies. Concrete implementations can provide their own list
 * of candidate shards to try the read operation on.
 */
@Slf4j
public abstract class TransportSingleShardAction<Request extends SingleShardRequest, Response extends ActionResponse> extends TransportAction<Request, Response> {

    @Resource
    ClusterService clusterService;
    @Resource
    TransportService transportService;

    final String transportShardAction;
    final String executor;
    Class<Request> request;

    protected TransportSingleShardAction(Settings settings, String actionName, Class<Request> request, String executor) {
        super(settings, actionName);
        this.request = request;
        this.transportShardAction = actionName + "[s]"; //加[s]代表是shard的行为 比如 indices:data/read/get[s] 要操作lucene
        this.executor = executor;

    }

    @PostConstruct
    private void init() {
        if (!isSubAction()) {
            //multi的 TransportShardMultiGetAction、TransportShardMultiPercolateAction、TransportShardMultiTermsVectorAction 都不注册这个
            transportService.registerRequestHandler(actionName, request, "same", new TransportHandler());
        }
        transportService.registerRequestHandler(transportShardAction, request, executor, new ShardTransportHandler());
    }

    /**
     * Tells whether the action is a main one or a subaction. Used to decide whether we need to register
     * the main transport handler. In fact if the action is a subaction, its execute method
     * will be called locally to its parent action.
     */
    protected boolean isSubAction() {
        return false;
    }

    @Override
    protected void doExecute(Request request, ActionListener<Response> listener) {
//        logTrace(LogActionType.SINGLE_SHARD,"doExecute begin request={},listener={}", request, listener);
//        new AsyncSingleAction(request, listener).start();
//        logTrace(LogActionType.SINGLE_SHARD,"doExecute end request={},listener={}", request, listener);
    }

    protected abstract Response shardOperation(Request request, ShardId shardId);

    protected abstract Response newResponse();

    protected abstract boolean resolveIndex(Request request);

//    protected ClusterBlockException checkGlobalBlock(ClusterState state) {
//        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
//    }
//
//    protected ClusterBlockException checkRequestBlock(ClusterState state, InternalRequest request) {
//        return state.blocks().indexBlockedException(ClusterBlockLevel.READ, request.concreteIndex());
//    }
//
//    protected void resolveRequest(ClusterState state, InternalRequest request) {
//
//    }
//
//    /**
//     * Returns the candidate shards to execute the operation on or <code>null</code> the execute
//     * the operation locally (the node that received the request)
//     */
//    @Nullable
//    protected abstract ShardsIterator shards(ClusterState state, InternalRequest request);
//
//    class AsyncSingleAction {
//
//        private final ActionListener<Response> listener;
//        private final ShardsIterator shardIt;
//        private final InternalRequest internalRequest;
//        private final DiscoveryNodes nodes;
//        private volatile Throwable lastFailure;
//
//        private AsyncSingleAction(Request request, ActionListener<Response> listener) {
//            this.listener = listener;
//
//            //当前集群状态
//            ClusterState clusterState = clusterService.state();
//            if (logger.isTraceEnabled()) {
//                logger.trace("executing [{}] based on cluster state version [{}]", request, clusterState.version());
//            }
//            //当前集群的节点信息。
//            nodes = clusterState.nodes();
//            //检测是否需要hold (不允许读)
//            ClusterBlockException blockException = checkGlobalBlock(clusterState);
//            if (blockException != null) {
//                throw blockException;
//            }
//
//            String concreteSingleIndex;
//            //如果需要解析index
//            if (resolveIndex(request)) {
//                concreteSingleIndex = indexNameExpressionResolver.concreteSingleIndex(clusterState, request);
//            } else {
//                concreteSingleIndex = request.index();
//            }
//            this.internalRequest = new InternalRequest(request, concreteSingleIndex);
//            resolveRequest(clusterState, internalRequest);
//
//            blockException = checkRequestBlock(clusterState, internalRequest);
//            if (blockException != null) {
//                throw blockException;
//            }
//
//            //根据请求找到所需对应的shard迭代器 (根据index,  type,  id,  routing, preference)
//            this.shardIt = shards(clusterState, internalRequest);
//        }
//
//        public void start() {
//            if (shardIt == null) {
//                /**
//                 * 如果没找到对应shrad 就把请求发送到本地
//                 */
//                // just execute it on the local node
//                transportService.sendRequest(clusterService.localNode(), transportShardAction, internalRequest.request(), new BaseTransportResponseHandler<Response>() {
//                    @Override
//                    public Response newInstance() {
//                        return newResponse();
//                    }
//
//                    @Override
//                    public String executor() {
//                        return ThreadPool.Names.SAME;
//                    }
//
//                    @Override
//                    public void handleResponse(final Response response) {
//                        listener.onResponse(response);
//                    }
//
//                    @Override
//                    public void handleException(TransportException exp) {
//                        perform(exp);
//                    }
//                });
//            } else {
//                perform(null);
//            }
//        }
//
//        private void onFailure(ShardRouting shardRouting, Throwable e) {
//            if (logger.isTraceEnabled() && e != null) {
//                logger.trace("{}: failed to execute [{}]", e, shardRouting, internalRequest.request());
//            }
//            perform(e);
//        }
//
//        private void perform(@Nullable final Throwable currentFailure) {
//            Throwable lastFailure = this.lastFailure;
//            if (lastFailure == null || TransportActions.isReadOverrideException(currentFailure)) {
//                lastFailure = currentFailure;
//                this.lastFailure = currentFailure;
//            }
//            final ShardRouting shardRouting = shardIt.nextOrNull();
//            if (shardRouting == null) {
//                Throwable failure = lastFailure;
//                if (failure == null || isShardNotAvailableException(failure)) {
//                    failure = new NoShardAvailableActionException(null, LoggerMessageFormat.format("No shard available for [{}]", internalRequest.request()), failure);
//                } else {
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("{}: failed to execute [{}]", failure, null, internalRequest.request());
//                    }
//                }
//                listener.onFailure(failure);
//                return;
//            }
//            DiscoveryNode node = nodes.get(shardRouting.currentNodeId());
//            if (node == null) {
//                onFailure(shardRouting, new NoShardAvailableActionException(shardRouting.shardId()));
//            } else {
//                internalRequest.request().internalShardId = shardRouting.shardId();
//                if (logger.isTraceEnabled()) {
//                    logger.trace(
//                            "sending request [{}] to shard [{}] on node [{}]",
//                            internalRequest.request(),
//                            internalRequest.request().internalShardId,
//                            node
//                    );
//                }
//                transportService.sendRequest(node, transportShardAction, internalRequest.request(), new BaseTransportResponseHandler<Response>() {
//
//                    @Override
//                    public Response newInstance() {
//                        return newResponse();
//                    }
//
//                    @Override
//                    public String executor() {
//                        return ThreadPool.Names.SAME;
//                    }
//
//                    @Override
//                    public void handleResponse(final Response response) {
//                        listener.onResponse(response);
//                    }
//
//                    @Override
//                    public void handleException(TransportException exp) {
//                        onFailure(shardRouting, exp);
//                    }
//                });
//            }
//        }
//    }

    /**
     * 不带s的action
     */
    private class TransportHandler implements TransportRequestHandler<Request> {

        @Override
        public void messageReceived(Request request, final TransportChannel channel) throws Exception {
            // if we have a local operation, execute it on a thread since we don't spawn
            request.operationThreaded(true);
            execute(request, new ActionListener<Response>() {
                @Override
                public void onResponse(Response result) {
                    try {
                        channel.sendResponse(result);
                    } catch (Throwable e) {
                        onFailure(e);
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    try {
                        channel.sendResponse(e);
                    } catch (Exception e1) {
                        log.warn("failed to send response for get", e1);
                    }
                }
            });
        }
    }

    /**
     * 带s的action
     */
    private class ShardTransportHandler implements TransportRequestHandler<Request> {

        @Override
        public void messageReceived(final Request request, final TransportChannel channel) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("executing [{}] on shard [{}]", request, request.internalShardId);
            }
//            logTrace(LogActionType.SINGLE_SHARD,"messageReceived begin request={},channel={}", request, channel);
            Response response = shardOperation(request, request.internalShardId);
            //如果是远程调用是NettyTransportChannel 如果是本地(或数据节点自己拿数据)则是DirectResponseChannel(异步)
            channel.sendResponse(response);
//            logTrace(LogActionType.SINGLE_SHARD,"messageReceived end request={},channel={}", request, channel);
        }
    }

    /**
     * Internal request class that gets built on each node. Holds the original request plus additional info.
     */
    protected class InternalRequest {
        final Request request;
        final String concreteIndex;

        InternalRequest(Request request, String concreteIndex) {
            this.request = request;
            this.concreteIndex = concreteIndex;
        }

        public Request request() {
            return request;
        }

        public String concreteIndex() {
            return concreteIndex;
        }
    }
}
