/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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

import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.fetcher.FetchCachedPipeline;
import org.mongopipe.core.fetcher.FetchPipeline;
import org.mongopipe.core.fetcher.FetchPipelineStore;
import org.mongopipe.core.model.PipelineRun;
import org.mongopipe.core.notifier.ChangeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the storage for MongoPipelines.
 * Use a store interface that might store them in any database (SQL/NoSQL), in memory cache or let the user provide his own implementation.
 * For in memory use a cache library or map implementation(but without collisions, unlike Java default Map implementations). By default
 * disable cache.
 *
 */
public class PipelineStore {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineStore.class);
  private Map<String, PipelineRun> store = new HashMap<>(); // TODO: Extract an interface allowing client his own implementation of storage location.
  private ChangeNotifier changeNotifier = new ChangeNotifier();

  private PipelineRunConfig pipelineRunConfig;
  private final FetchPipeline<PipelineRun> fetchPipeline;

  public PipelineStore(PipelineRunConfig pipelineRunConfig) {

    this.pipelineRunConfig = pipelineRunConfig;
    //check to update or not cache
    FetchPipelineStore<PipelineRun> cachePipelineStore = new FetchPipelineStore<>(pipelineRunConfig, PipelineRun.class);
    this.fetchPipeline =
            pipelineRunConfig.isStoreCacheEnabled() ? new FetchCachedPipeline<>(cachePipelineStore) : cachePipelineStore;
    changeNotifier.addListener((event) -> fetchPipeline.update());
  }

  public PipelineRun getPipeline(String pipelineId) {
    // TODO: versioning, cache, exception if not found, etc

    //TODO IOPE: add change listener/notifier
    return fetchPipeline.getById(pipelineId);
  }

  public void createPipeline(PipelineRun pipelineRun) {
    // TODO: versioning, cache, exception if not found, etc
    pipelineRunConfig.getMongoDatabase().getCollection(pipelineRunConfig.getStoreCollection(), PipelineRun.class)
        .insertOne(pipelineRun);
    changeNotifier.fire();
  }

  public void update(PipelineRun pipelineRun) {
    // TODO: On each update increment Pipeline#version.

    changeNotifier.fire();
  }
}
