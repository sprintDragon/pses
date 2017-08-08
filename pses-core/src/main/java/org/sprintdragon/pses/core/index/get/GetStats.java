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

/**
 */
public class GetStats {

    private long existsCount;
    private long existsTimeInMillis;
    private long missingCount;
    private long missingTimeInMillis;
    private long current;

    public GetStats() {
    }

    public GetStats(long existsCount, long existsTimeInMillis, long missingCount, long missingTimeInMillis, long current) {
        this.existsCount = existsCount;
        this.existsTimeInMillis = existsTimeInMillis;
        this.missingCount = missingCount;
        this.missingTimeInMillis = missingTimeInMillis;
        this.current = current;
    }

    public void add(GetStats stats) {
        if (stats == null) {
            return;
        }
        current += stats.current;
        addTotals(stats);
    }

    public void addTotals(GetStats stats) {
        if (stats == null) {
            return;
        }
        existsCount += stats.existsCount;
        existsTimeInMillis += stats.existsTimeInMillis;
        missingCount += stats.missingCount;
        missingTimeInMillis += stats.missingTimeInMillis;
        current += stats.current;
    }

    public long getCount() {
        return existsCount + missingCount;
    }

    public long getTimeInMillis() {
        return existsTimeInMillis + missingTimeInMillis;
    }

    public long getExistsCount() {
        return this.existsCount;
    }

    public long getExistsTimeInMillis() {
        return this.existsTimeInMillis;
    }

    public long getMissingCount() {
        return this.missingCount;
    }

    public long getMissingTimeInMillis() {
        return this.missingTimeInMillis;
    }

    public long current() {
        return this.current;
    }

}
