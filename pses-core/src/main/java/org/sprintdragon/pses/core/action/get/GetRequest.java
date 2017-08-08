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


import com.sun.istack.internal.Nullable;
import org.sprintdragon.pses.core.action.ActionRequest;
import org.sprintdragon.pses.core.action.RealtimeRequest;
import org.sprintdragon.pses.core.action.ValidateActions;
import org.sprintdragon.pses.core.action.exception.ActionRequestValidationException;
import org.sprintdragon.pses.core.action.supprot.single.shard.SingleShardRequest;
import org.sprintdragon.pses.core.common.lucene.uid.Versions;
import org.sprintdragon.pses.core.index.VersionType;
import org.sprintdragon.pses.core.search.fetch.source.FetchSourceContext;

public class GetRequest extends SingleShardRequest<GetRequest> implements RealtimeRequest {

    private String type;
    private String id;
    private String routing;
    private String preference;

    private String[] fields;

    private FetchSourceContext fetchSourceContext;

    private boolean refresh = false;

    Boolean realtime;

    private VersionType versionType = VersionType.INTERNAL;
    private long version = Versions.MATCH_ANY;
    private boolean ignoreErrorsOnGeneratedFields;

//    GetRequest() {
//        type = "_all";
//    }

    /**
     * Copy constructor that creates a new get request that is a copy of the one provided as an argument.
     * The new request will inherit though headers and context from the original request that caused it.
     */
    public GetRequest(GetRequest getRequest, ActionRequest originalRequest) {
        super(originalRequest, getRequest.index);
        this.index = getRequest.index;
        this.type = getRequest.type;
        this.id = getRequest.id;
        this.routing = getRequest.routing;
        this.preference = getRequest.preference;
        this.fields = getRequest.fields;
        this.fetchSourceContext = getRequest.fetchSourceContext;
        this.refresh = getRequest.refresh;
        this.realtime = getRequest.realtime;
        this.version = getRequest.version;
        this.versionType = getRequest.versionType;
        this.ignoreErrorsOnGeneratedFields = getRequest.ignoreErrorsOnGeneratedFields;
    }

    public GetRequest() {
        this.type = "_all";
    }

    /**
     * Constructs a new get request against the specified index. The {@link #type(String)} and {@link #id(String)}
     * must be set.
     */
    public GetRequest(String index) {
        super(index);
        this.type = "_all";
    }

    /**
     * Constructs a new get request starting from the provided request, meaning that it will
     * inherit its headers and context, and against the specified index.
     */
    public GetRequest(ActionRequest request, String index) {
        super(request, index);
    }

//    /**
//     * Constructs a new get request against the specified index with the type and id.
//     *
//     * @param index The index to get the document from
//     * @param type  The type of the document
//     * @param id    The id of the document
//     */
//    public GetRequest(String index, String type, String id) {
//        super(index);
//        this.type = type;
//        this.id = id;
//    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validateNonNullIndex();
        if (type == null) {
            validationException = ValidateActions.addValidationError("type is missing", validationException);
        }
        if (id == null) {
            validationException = ValidateActions.addValidationError("id is missing", validationException);
        }
//        if (!versionType.validateVersionForReads(version)) {
//            validationException = ValidateActions.addValidationError("illegal version value [" + version + "] for version type [" + versionType.name() + "]",
//                    validationException);
//        }
        return validationException;
    }

    /**
     * Sets the type of the document to fetch.
     */
    public GetRequest type(@Nullable String type) {
        if (type == null) {
            type = "_all";
        }
        this.type = type;
        return this;
    }

    /**
     * Sets the id of the document to fetch.
     */
    public GetRequest id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the parent id of this document. Will simply set the routing to this value, as it is only
     * used for routing with delete requests.
     */
    public GetRequest parent(String parent) {
        if (routing == null) {
            routing = parent;
        }
        return this;
    }

    /**
     * Controls the shard routing of the request. Using this value to hash the shard
     * and not the id.
     */
    public GetRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    /**
     * Sets the preference to execute the search. Defaults to randomize across shards. Can be set to
     * <tt>_local</tt> to prefer local shards, <tt>_primary</tt> to execute only on primary shards, or
     * a custom value, which guarantees that the same order will be used across different requests.
     */
    public GetRequest preference(String preference) {
        this.preference = preference;
        return this;
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    public String routing() {
        return this.routing;
    }

    public String preference() {
        return this.preference;
    }

    /**
     * Allows setting the {@link FetchSourceContext} for this request, controlling if and how _source should be returned.
     */
    public GetRequest fetchSourceContext(FetchSourceContext context) {
        this.fetchSourceContext = context;
        return this;
    }

    public FetchSourceContext fetchSourceContext() {
        return fetchSourceContext;
    }

    /**
     * Explicitly specify the fields that will be returned. By default, the <tt>_source</tt>
     * field will be returned.
     */
    public GetRequest fields(String... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Explicitly specify the fields that will be returned. By default, the <tt>_source</tt>
     * field will be returned.
     */
    public String[] fields() {
        return this.fields;
    }

    /**
     * Should a refresh be executed before this get operation causing the operation to
     * return the latest value. Note, heavy get should not set this to <tt>true</tt>. Defaults
     * to <tt>false</tt>.
     */
    public GetRequest refresh(boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    public boolean refresh() {
        return this.refresh;
    }

    public boolean realtime() {
        return this.realtime == null ? true : this.realtime;
    }

    @Override
    public GetRequest realtime(Boolean realtime) {
        this.realtime = realtime;
        return this;
    }

    /**
     * Sets the version, which will cause the get operation to only be performed if a matching
     * version exists and no changes happened on the doc since then.
     */
    public long version() {
        return version;
    }

    public GetRequest version(long version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the versioning type. Defaults to {@link VersionType#INTERNAL}.
     */
    public GetRequest versionType(VersionType versionType) {
        this.versionType = versionType;
        return this;
    }

    public GetRequest ignoreErrorsOnGeneratedFields(boolean ignoreErrorsOnGeneratedFields) {
        this.ignoreErrorsOnGeneratedFields = ignoreErrorsOnGeneratedFields;
        return this;
    }

    public VersionType versionType() {
        return this.versionType;
    }

    public boolean ignoreErrorsOnGeneratedFields() {
        return ignoreErrorsOnGeneratedFields;
    }

    @Override
    public String toString() {
        return "get [" + index + "][" + type + "][" + id + "]: routing [" + routing + "]";
    }

}
