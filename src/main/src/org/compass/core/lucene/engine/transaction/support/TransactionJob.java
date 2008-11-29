/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.core.lucene.engine.transaction.support;

import java.io.Serializable;

import org.apache.lucene.index.Term;
import org.compass.core.engine.SearchEngineFactory;
import org.compass.core.spi.InternalResource;
import org.compass.core.spi.ResourceKey;

/**
 * A transaction job represents a job that needs to be performed.
 *
 * @author kimchy
 */
public class TransactionJob implements Serializable {

    /**
     * The type of the transaction job. Can be create, delete, or update.
     */
    public static enum Type {
        CREATE,
        DELETE,
        UPDATE
    }

    private final Type type;

    private final ResourceKey resourceKey;

    private final InternalResource resource;

    private final String resourceUID;

    public TransactionJob(Type type, InternalResource resource) {
        this.type = type;
        this.resource = resource;
        this.resourceKey = resource.getResourceKey();
        this.resourceUID = resourceKey.buildUID();
    }

    public TransactionJob(Type type, ResourceKey resourceKey) {
        this.type = type;
        this.resourceKey = resourceKey;
        this.resource = null;
        this.resourceUID = resourceKey.buildUID();
    }

    public Type getType() {
        return type;
    }

    public ResourceKey getResourceKey() {
        return resourceKey;
    }

    public InternalResource getResource() {
        if (resource == null) {
            throw new IllegalStateException("No resource provided for type [" + type + "] and resource key [" + resourceKey + "]");
        }
        return resource;
    }

    public String getResourceUID() {
        return resourceUID;
    }

    public String getSubIndex() {
        return resourceKey.getSubIndex();
    }

    public Term getUIDTerm() {
        return new Term(resourceKey.getUIDPath(), resourceUID);        
    }

    public void attach(SearchEngineFactory searchEngineFactory) {
        if (resource != null) {
            resource.attach(searchEngineFactory);
        }
        resourceKey.attach(searchEngineFactory);
    }

    @Override
    public String toString() {
        if (resource == null) return "Operation [" + type + "] Key [" + resourceKey + "]";
        return "Operation [" + type + "] Key [" + resourceKey + "] Resource [" + resource + "]";
    }
}