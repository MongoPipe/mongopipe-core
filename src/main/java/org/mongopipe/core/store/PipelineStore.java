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

import org.mongopipe.core.config.PipelineRunContext;
import org.mongopipe.core.fetcher.FetchCachedPipeline;
import org.mongopipe.core.fetcher.FetchPipeline;
import org.mongopipe.core.fetcher.FetchPipelineStore;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.notifier.ChangeNotifier;
import org.mongopipe.core.util.BsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mongopipe.core.util.BsonUtil.toBsonList;

/**
 * Handles the storage for MongoPipelines.
 * Use a store interface that might store them in any database (SQL/NoSQL), in memory cache or let the user provide his own implementation.
 * For in memory use a cache library or map implementation(but without collisions, unlike Java default Map implementations). By default
 * disable cache.
 *
 */
public class PipelineStore {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineStore.class);

  private PipelineRunContext pipelineRunContext;
  private final FetchPipeline<Pipeline> fetchPipeline;
  private ChangeNotifier changeNotifier = new ChangeNotifier();

  public PipelineStore(PipelineRunContext pipelineRunContext) {
    this.pipelineRunContext = pipelineRunContext;

    //check to update or not cache
    FetchPipelineStore<Pipeline> cachePipelineStore = new FetchPipelineStore<>(pipelineRunContext, Pipeline.class);
    this.fetchPipeline = pipelineRunContext.getPipelineRunConfig().isStoreCacheEnabled()
        ? new FetchCachedPipeline<>(cachePipelineStore) : cachePipelineStore;
    changeNotifier.addListener((event) -> fetchPipeline.update());
  }

  public Pipeline getPipeline(String pipelineId) {
    // TODO: versioning, cache, exception if not found, etc

    //TODO IOPE: add change listener/notifier
    return fetchPipeline.getById(pipelineId);
  }

  public void createPipeline(Pipeline pipeline) {
    enhance(pipeline);
    // TODO: versioning, cache, exception if not found, etc
    pipelineRunContext.getMongoDatabase().getCollection(pipelineRunContext.getPipelineRunConfig().getStoreCollection(), Pipeline.class)
        .insertOne(pipeline);
    changeNotifier.fire();

    LOG.info("Created pipeline: {}", pipeline.getId());
  }

  public void update(Pipeline pipeline) {
    enhance(pipeline);
    // TODO: On each update increment Pipeline#version.
    changeNotifier.fire();

    LOG.info("Updated pipeline: {}", pipeline.getId());
  }

  private void enhance(Pipeline pipeline) {
    if (pipeline.getPipeline() != null) {
      pipeline.setPipelineAsString(BsonUtil.toString(pipeline.getPipeline()));
    } else if (pipeline.getPipelineAsString() != null) {
      pipeline.setPipeline(toBsonList(pipeline.getPipelineAsString())); // Important: Store as native BsonDocument list in MongoDB and not as a String.
    }
  }
}
