/*
 * Copyright 2004-2006 the original author or authors.
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

package org.compass.core.lucene.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.compass.core.CompassTransaction.TransactionIsolation;
import org.compass.core.Resource;
import org.compass.core.config.CompassSettings;
import org.compass.core.config.RuntimeCompassSettings;
import org.compass.core.engine.SearchEngine;
import org.compass.core.engine.SearchEngineAnalyzerHelper;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.engine.SearchEngineHits;
import org.compass.core.engine.SearchEngineInternalSearch;
import org.compass.core.engine.SearchEngineQuery;
import org.compass.core.engine.SearchEngineQueryBuilder;
import org.compass.core.engine.SearchEngineQueryFilterBuilder;
import org.compass.core.engine.SearchEngineTermFrequencies;
import org.compass.core.engine.event.SearchEngineEventManager;
import org.compass.core.lucene.engine.query.LuceneSearchEngineQueryBuilder;
import org.compass.core.lucene.engine.query.LuceneSearchEngineQueryFilterBuilder;
import org.compass.core.lucene.engine.transaction.LuceneSearchEngineTransaction;
import org.compass.core.lucene.engine.transaction.lucene.LuceneTransaction;
import org.compass.core.lucene.engine.transaction.readcommitted.ReadCommittedTransaction;
import org.compass.core.lucene.engine.transaction.serializable.SerializableTransaction;
import org.compass.core.lucene.util.LuceneUtils;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.spi.InternalResource;
import org.compass.core.spi.MultiResource;
import org.compass.core.spi.ResourceKey;
import org.compass.core.util.StringUtils;

/**
 * @author kimchy
 */
public class LuceneSearchEngine implements SearchEngine {

    protected final static Log log = LogFactory.getLog(LuceneSearchEngine.class);

    private static final int UNKNOWN = -1;

    private static final int STARTED = 0;

    private static final int COMMIT = 1;

    private static final int ROLLBACK = 2;

    private int transactionState;

    private LuceneSearchEngineTransaction transaction;

    private LuceneSearchEngineFactory searchEngineFactory;

    private SearchEngineEventManager eventManager = new SearchEngineEventManager();

    private boolean readOnly;

    private RuntimeCompassSettings runtimeSettings;

    public LuceneSearchEngine(RuntimeCompassSettings runtimeSettings, LuceneSearchEngineFactory searchEngineFactory) {
        this.runtimeSettings = runtimeSettings;
        this.readOnly = true;
        this.searchEngineFactory = searchEngineFactory;
        this.transactionState = UNKNOWN;
        eventManager.registerLifecycleListener(searchEngineFactory.getEventManager());
        searchEngineFactory.getLuceneIndexManager().getStore().registerEventListeners(this, eventManager);
    }

    public SearchEngineQueryBuilder queryBuilder() throws SearchEngineException {
        return new LuceneSearchEngineQueryBuilder(this);
    }

    public SearchEngineQueryFilterBuilder queryFilterBuilder() throws SearchEngineException {
        return new LuceneSearchEngineQueryFilterBuilder();
    }

    public SearchEngineAnalyzerHelper analyzerHelper() {
        return new LuceneSearchEngineAnalyzerHelper(this);
    }

    public void begin() throws SearchEngineException {
        this.readOnly = true;
        if (transactionState == STARTED) {
            throw new SearchEngineException("Transaction already started, why start it again?");
        }

        Class transactionIsolationClass = searchEngineFactory.getLuceneSettings().getTransactionIsolationClass();
        if (transactionIsolationClass != null) {
            transactionState = UNKNOWN;
            try {
                transaction = (LuceneSearchEngineTransaction) transactionIsolationClass.newInstance();
            } catch (Exception e) {
                throw new SearchEngineException("Failed to create an instance for transaction ["
                        + transactionIsolationClass.getName() + "]", e);
            }
            transaction.configure(this);
            eventManager.beforeBeginTransaction();
            transaction.begin();
            eventManager.afterBeginTransaction();
            transactionState = STARTED;
            return;
        }
        TransactionIsolation transactionIsolation = searchEngineFactory.getLuceneSettings().getTransactionIsolation();
        begin(transactionIsolation);
    }

    public void begin(TransactionIsolation transactionIsolation) throws SearchEngineException {
        if (transactionState == STARTED) {
            throw new SearchEngineException("Transaction already started, why start it again?");
        }
        transactionState = UNKNOWN;
        this.readOnly = true;
        if (transactionIsolation == null) {
            transactionIsolation = searchEngineFactory.getLuceneSettings().getTransactionIsolation();
        }
        if (transactionIsolation == TransactionIsolation.READ_COMMITTED) {
            transaction = new ReadCommittedTransaction();
        } else if (transactionIsolation == TransactionIsolation.READ_ONLY_READ_COMMITTED) {
            transaction = new ReadCommittedTransaction();
        } else if (transactionIsolation == TransactionIsolation.BATCH_INSERT) {
            transaction = new LuceneTransaction();
        } else if (transactionIsolation == TransactionIsolation.LUCENE) {
            transaction = new LuceneTransaction();
        } else if (transactionIsolation == TransactionIsolation.SERIALIZABLE) {
            transaction = new SerializableTransaction();
        }
        transaction.configure(this);
        eventManager.beforeBeginTransaction();
        transaction.begin();
        eventManager.afterBeginTransaction();
        transactionState = STARTED;
    }

    public void verifyWithinTransaction() throws SearchEngineException {
        if (transactionState != STARTED) {
            throw new SearchEngineException(
                    "Search engine transaction not successfully started or already committed/rolledback");
        }
    }

    public boolean isWithinTransaction() throws SearchEngineException {
        return transactionState == STARTED;
    }

    public void prepare() throws SearchEngineException {
        verifyWithinTransaction();
        if (transaction != null) {
            transaction.prepare();
        }
        eventManager.afterPrepare();
    }

    public void commit(boolean onePhase) throws SearchEngineException {
        verifyWithinTransaction();
        if (transaction != null) {
            transaction.commit(onePhase);
            eventManager.afterCommit(onePhase);
        }
        transaction = null;
        transactionState = COMMIT;
    }

    public void rollback() throws SearchEngineException {
        verifyWithinTransaction();
        try {
            if (transaction != null) {
                try {
                    transaction.rollback();
                } finally {
                    eventManager.afterRollback();
                }
            }
        } finally {
            transaction = null;
            transactionState = ROLLBACK;
        }
    }

    public void flush() throws SearchEngineException {
        verifyWithinTransaction();
        if (transaction != null) {
            transaction.flush();
        }
    }

    public boolean wasRolledBack() throws SearchEngineException {
        return transactionState == ROLLBACK;
    }

    public boolean wasCommitted() throws SearchEngineException {
        return transactionState == COMMIT;
    }

    public void close() throws SearchEngineException {
        eventManager.close();
        if (transactionState == STARTED) {
            log.warn("Transaction not committed/rolled backed, rolling back");
            try {
                rollback();
            } catch (Exception e) {
                log.warn("Failed to rollback transcation, ignoring", e);
            }
        }
        eventManager = null;
    }

    public void delete(Resource resource) throws SearchEngineException {
        verifyWithinTransaction();
        readOnly = false;
        if (resource instanceof MultiResource) {
            MultiResource multiResource = (MultiResource) resource;
            for (int i = 0; i < multiResource.size(); i++) {
                delete(((InternalResource) multiResource.resource(i)).resourceKey());
            }
        } else {
            delete(((InternalResource) resource).resourceKey());
        }
    }

    private void delete(ResourceKey resourceKey) throws SearchEngineException {
        if (resourceKey.getIds().length == 0) {
            throw new SearchEngineException("Cannot delete a resource with no ids and alias [" + resourceKey.getAlias() + "]");
        }
        transaction.delete(resourceKey);
        String[] extendingAliases = resourceKey.getResourceMapping().getExtendingAliases();
        for (String extendingAlias : extendingAliases) {
            ResourceMapping extendingMapping = getSearchEngineFactory().getMapping().getMappingByAlias(extendingAlias);
            ResourceKey key = new ResourceKey(extendingMapping, resourceKey.getIds());
            transaction.delete(key);
        }
        if (log.isDebugEnabled()) {
            log.debug("RESOURCE DELETE {" + resourceKey.getAlias() + "} " + StringUtils.arrayToCommaDelimitedString(resourceKey.getIds()));
        }
    }

    public void save(Resource resource) throws SearchEngineException {
        readOnly = false;
        createOrUpdate(resource, true);
    }

    public void create(Resource resource) throws SearchEngineException {
        readOnly = false;
        createOrUpdate(resource, false);
    }

    private void createOrUpdate(final Resource resource, boolean update) throws SearchEngineException {
        verifyWithinTransaction();
        readOnly = false;
        String alias = resource.getAlias();
        ResourceMapping resourceMapping = searchEngineFactory.getMapping().getRootMappingByAlias(alias);
        if (resourceMapping == null) {
            throw new SearchEngineException("Failed to find mapping for alias [" + alias + "]");
        }
        if (resource instanceof MultiResource) {
            MultiResource multiResource = (MultiResource) resource;
            for (int i = 0; i < multiResource.size(); i++) {
                InternalResource resource1 = (InternalResource) multiResource.resource(i);
                Analyzer analyzer = enhanceResource(resourceMapping, resource1);
                if (update) {
                    transaction.update(resource1, analyzer);
                    if (log.isDebugEnabled()) {
                        log.debug("RESOURCE SAVE " + resource1);
                    }
                } else {
                    transaction.create(resource1, analyzer);
                    if (log.isDebugEnabled()) {
                        log.debug("RESOURCE CREATE " + resource1);
                    }
                }
            }
        } else {
            InternalResource resource1 = (InternalResource) resource;
            Analyzer analyzer = enhanceResource(resourceMapping, resource1);
            if (update) {
                transaction.update(resource1, analyzer);
                if (log.isDebugEnabled()) {
                    log.debug("RESOURCE SAVE " + resource1);
                }
            } else {
                transaction.create(resource1, analyzer);
                if (log.isDebugEnabled()) {
                    log.debug("RESOURCE CREATE " + resource1);
                }
            }
        }
    }

    private Analyzer enhanceResource(ResourceMapping resourceMapping, InternalResource resource) throws SearchEngineException {
        LuceneUtils.addExtendedProeprty(resource, resourceMapping, searchEngineFactory);
        LuceneUtils.applyBoostIfNeeded(resource, searchEngineFactory);
        Analyzer analyzer = searchEngineFactory.getAnalyzerManager().getAnalyzerByResource(resource);
        return LuceneUtils.addAllProperty(resource, analyzer, resource.resourceKey().getResourceMapping(), this);
    }

    public Resource get(Resource idResource) throws SearchEngineException {
        verifyWithinTransaction();
        ResourceKey resourceKey = ((InternalResource) idResource).resourceKey();
        if (resourceKey.getIds().length == 0) {
            throw new SearchEngineException("Cannot load a resource with no ids and alias [" + resourceKey.getAlias() + "]");
        }
        Resource[] result = transaction.get(resourceKey);
        if (result.length == 0) {
            // none directly, try and load polymorphic ones
            String[] extendingAliases = resourceKey.getResourceMapping().getExtendingAliases();
            for (String extendingAlias : extendingAliases) {
                ResourceMapping extendingMapping = getSearchEngineFactory().getMapping().getMappingByAlias(extendingAlias);
                ResourceKey key = new ResourceKey(extendingMapping, resourceKey.getIds());
                result = transaction.get(key);
                if (result.length > 0) {
                    return result[result.length - 1];
                }
            }
            // did not find in the extending aliases as well
            return null;
        } else if (result.length > 1) {
            log.warn("Found several matches in get/load operation for resource alias [" + resourceKey.getAlias() + "] and ids ["
                    + StringUtils.arrayToCommaDelimitedString(resourceKey.getIds()) + "]");
            return result[result.length - 1];
        }
        return result[0];
    }

    public Resource load(Resource idResource) throws SearchEngineException {
        String alias = idResource.getAlias();
        Resource resource = get(idResource);
        if (resource == null) {
            throw new SearchEngineException("Failed to find resource with alias [" + alias + "] and ids ["
                    + StringUtils.arrayToCommaDelimitedString(idResource.getIds()) + "]");
        }
        return resource;
    }

    public SearchEngineHits find(SearchEngineQuery query) throws SearchEngineException {
        verifyWithinTransaction();
        SearchEngineHits hits = transaction.find(query);
        if (log.isDebugEnabled()) {
            log.debug("RESOURCE QUERY [" + query + "] HITS [" + hits.getLength() + "]");
        }
        return hits;
    }

    public SearchEngineTermFrequencies termFreq(String[] propertyNames, int size, SearchEngineInternalSearch internalSearch) {
        return new LuceneSearchEngineTermFrequencies(propertyNames, size, (LuceneSearchEngineInternalSearch) internalSearch);
    }

    public SearchEngineInternalSearch internalSearch(String[] subIndexes, String[] aliases) throws SearchEngineException {
        verifyWithinTransaction();
        return transaction.internalSearch(subIndexes, aliases);
    }

    public LuceneSearchEngineFactory getSearchEngineFactory() {
        return searchEngineFactory;
    }

    public CompassSettings getSettings() {
        return runtimeSettings;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }
}