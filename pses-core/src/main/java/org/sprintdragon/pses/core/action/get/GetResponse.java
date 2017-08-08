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

import org.sprintdragon.pses.core.action.ActionResponse;
import org.sprintdragon.pses.core.index.get.GetField;
import org.sprintdragon.pses.core.index.get.GetResult;

import java.util.Iterator;
import java.util.Map;

/**
 * The response of a get action.
 *
 * @see GetRequest
 */
public class GetResponse extends ActionResponse implements Iterable<GetField> {

    private GetResult getResult;

    GetResponse() {
    }

    public GetResponse(GetResult getResult) {
        this.getResult = getResult;
    }

    /**
     * Does the document exists.
     */
    public boolean isExists() {
        return getResult.isExists();
    }

    /**
     * The index the document was fetched from.
     */
    public String getIndex() {
        return getResult.getIndex();
    }

    /**
     * The type of the document.
     */
    public String getType() {
        return getResult.getType();
    }

    /**
     * The id of the document.
     */
    public String getId() {
        return getResult.getId();
    }

    /**
     * The version of the doc.
     */
    public long getVersion() {
        return getResult.getVersion();
    }

    /**
     * The source of the document if exists.
     */
    public byte[] getSourceAsBytes() {
        return getResult.source();
    }

    public Map<String, GetField> getFields() {
        return getResult.getFields();
    }

    public GetField getField(String name) {
        return getResult.field(name);
    }

    @Override
    public Iterator<GetField> iterator() {
        return getResult.iterator();
    }

}
