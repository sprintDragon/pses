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

package org.sprintdragon.pses.core.index.get;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 */
public class GetResult implements Iterable<GetField> {

    private String index;
    private String type;
    private String id;
    private long version;
    private boolean exists;
    private Map<String, GetField> fields;
    private Map<String, Object> sourceAsMap;
    //    private BytesReference source;
    private byte[] sourceAsBytes;

    GetResult() {
    }

    public GetResult(String index, String type, String id, long version, boolean exists, Map<String, GetField> fields) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.version = version;
        this.exists = exists;
//        this.source = source;
        this.fields = fields;
        if (this.fields == null) {
            this.fields = ImmutableMap.of();
        }
    }

    /**
     * Does the document exists.
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * The index the document was fetched from.
     */
    public String getIndex() {
        return index;
    }

    /**
     * The type of the document.
     */
    public String getType() {
        return type;
    }

    /**
     * The id of the document.
     */
    public String getId() {
        return id;
    }

    /**
     * The version of the doc.
     */
    public long getVersion() {
        return version;
    }

    /**
     * The source of the document if exists.
     */
    public byte[] source() {
//        if (source == null) {
//            return null;
//        }
        if (sourceAsBytes != null) {
            return sourceAsBytes;
        } else {
            return null;
        }
//        this.sourceAsBytes = sourceRef().toBytes();
//        return this.sourceAsBytes;
    }

//    /**
//     * Returns bytes reference, also un compress the source if needed.
//     */
//    public BytesReference sourceRef() {
//        try {
//            this.source = CompressorFactory.uncompressIfNeeded(this.source);
//            return this.source;
//        } catch (IOException e) {
//            throw new ElasticsearchParseException("failed to decompress source", e);
//        }
//    }

    /**
     * Internal source representation, might be compressed....
     */
//    public BytesReference internalSourceRef() {
//        return source;
//    }

    /**
     * Is the source empty (not available) or not.
     */
//    public boolean isSourceEmpty() {
//        return source == null;
//    }

//    /**
//     * The source of the document (as a string).
//     */
//    public String sourceAsString() {
//        if (source == null) {
//            return null;
//        }
//        BytesReference source = sourceRef();
//        try {
//            return XContentHelper.convertToJson(source, false);
//        } catch (IOException e) {
//            throw new ElasticsearchParseException("failed to convert source to a json string");
//        }
//    }

//    /**
//     * The source of the document (As a map).
//     */
//    @SuppressWarnings({"unchecked"})
//    public Map<String, Object> sourceAsMap() throws ElasticsearchParseException {
//        if (source == null) {
//            return null;
//        }
//        if (sourceAsMap != null) {
//            return sourceAsMap;
//        }
//
//        sourceAsMap = SourceLookup.sourceAsMap(source);
//        return sourceAsMap;
//    }

//    public Map<String, Object> getSource() {
//        return sourceAsMap();
//    }
    public Map<String, GetField> getFields() {
        return fields;
    }

    public GetField field(String name) {
        return fields.get(name);
    }

    @Override
    public Iterator<GetField> iterator() {
        if (fields == null) {
            return Collections.emptyIterator();
        }
        return fields.values().iterator();
    }


}

