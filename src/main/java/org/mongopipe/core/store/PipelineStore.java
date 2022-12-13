/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongopipe.core.store;

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.exception.PipelineNotFoundException;
import org.mongopipe.core.fetcher.FetchCachedPipeline;
import org.mongopipe.core.fetcher.FetchPipeline;
import org.mongopipe.core.fetcher.FetchPipelineStore;
import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.notifier.GenericChangeNotifier;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.util.BsonUtil;

import java.time.LocalDateTime;

import static org.mongopipe.core.util.BsonUtil.toBsonList;

/**
 * Handles the storage for MongoPipelines.
 * Use a store interface that might store them in any database (SQL/NoSQL), in memory cache or let the user provide his own implementation.
 * For in memory use a cache library or map implementation(but without collisions, unlike Java default Map implementations). By default
 * disable cache.
 *
 */
public class PipelineStore {
  private static final Log LOG = CustomLogFactory.getLogger(PipelineStore.class);
  private MongoPipeConfig mongoPipeConfig;
  private final FetchPipeline fetchPipeline;
  private GenericChangeNotifier changeNotifier = new GenericChangeNotifier();

  private final PipelineCrudStore crudStore;
  private final PipelineHistoryStore historyStore;

  public PipelineStore(RunContext runContext) {
    mongoPipeConfig = runContext.getMongoPipeConfig();
    crudStore = Stores.from(PipelineCrudStore.class);
    historyStore = Stores.from(PipelineHistoryStore.class);

    //check to update or not cache
    FetchPipelineStore cachePipelineStore = new FetchPipelineStore(crudStore);
    this.fetchPipeline = runContext.getMongoPipeConfig().isStoreCacheEnabled()
        ? new FetchCachedPipeline(cachePipelineStore) : cachePipelineStore;

    changeNotifier.addListener((event) -> fetchPipeline.update());
  }

  public Pipeline getPipeline(String pipelineId) {
    return fetchPipeline.getById(pipelineId);
  }

  public Pipeline create(Pipeline pipeline) {
    validateAndEnhance(pipeline);
    pipeline.setVersion(1L);
    pipeline.setCreatedAt(LocalDateTime.now());
    pipeline.setUpdatedAt(pipeline.getCreatedAt());
    Pipeline createdPipeline = crudStore.save(pipeline);
    changeNotifier.fire();

    LOG.info("Created pipeline: {}", pipeline.getId());
    return createdPipeline;
  }

  public Pipeline update(Pipeline pipeline) {
    validateAndEnhance(pipeline);

    String pipelineId = pipeline.getId();
    LocalDateTime now = LocalDateTime.now();
    if (pipelineId == null) {
      throw new MongoPipeConfigException("Pipeline id/name needs to be provided");
    }
    Pipeline old = getPipeline(pipelineId);
    if (old != null) {
      backup(old); // Save old first
      pipeline.setCreatedAt(old.getCreatedAt());
      pipeline.setVersion(old.getVersion() + 1);
    } else {
      // Allow upsert.
      pipeline.setCreatedAt(now);
      pipeline.setVersion(1L);
    }
    pipeline.setUpdatedAt(LocalDateTime.now());

    Pipeline updatedPipeline = crudStore.save(pipeline);

    changeNotifier.fire();
    LOG.info("Updated pipeline: {}", pipeline.getId());
    return updatedPipeline;
  }


  public void delete(Pipeline pipeline) {
    deleteById(pipeline.getId());
  }

  public void deleteById(String id) {
    Pipeline pipeline = getPipeline(id);
    if (pipeline == null) {
      throw new PipelineNotFoundException(id);
    }
    backup(pipeline);
    crudStore.deleteById(id);
    changeNotifier.fire();
    LOG.info("Deleted pipeline: {}", id);
  }

  private void backup(Pipeline pipeline) {
    if (mongoPipeConfig.isStoreHistoryEnabled()) {
      historyStore.save(pipeline);
    }
  }

  private void validateAndEnhance(Pipeline pipeline) {
    if (pipeline.getId() == null) {
      throw new MongoPipeConfigException("Pipeline id/name needs to be provided");
    }
    if (pipeline.getCollection() == null) {
      throw new MongoPipeConfigException("Collection name can not be null");
    }
    if (pipeline.getPipeline() != null) {
      pipeline.setPipelineAsString(BsonUtil.toString(pipeline.getPipeline()));
    } else if (pipeline.getPipelineAsString() != null) {
      pipeline.setPipeline(toBsonList(pipeline.getPipelineAsString())); // Important: Store as native BsonDocument list in MongoDB and not as a String.
    }
  }

  public Long count() {
    return crudStore.count();
  }

  public Iterable<Pipeline> findAll() {
    return crudStore.findAll();
  }
}
