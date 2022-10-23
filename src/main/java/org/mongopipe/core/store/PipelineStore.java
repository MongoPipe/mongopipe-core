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

import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.util.BsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static org.mongopipe.core.util.BsonUtil.toBsonDocumentList;

/**
 * Handles the storage for MongoPipelines.
 * Use a store interface that might store them in any database (SQL/NoSQL), in memory cache or let the user provide his own implementation.
 * For in memory use a cache library or map implementation(but without collisions, unlike Java default Map implementations). By default
 * disable cache.
 *
 */
public class PipelineStore {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineStore.class);
  private Map<String, Pipeline> store = new HashMap<>(); // TODO: Extract an interface allowing client his own implementation of storage location.

  private PipelineRunConfig pipelineRunConfig;

  public PipelineStore(PipelineRunConfig pipelineRunConfig) {
    this.pipelineRunConfig = pipelineRunConfig;
  }

  public Pipeline getPipeline(String pipelineId) {
    // TODO: versioning, cache, exception if not found, etc
    return pipelineRunConfig.getMongoDatabase().getCollection(pipelineRunConfig.getStoreCollection(), Pipeline.class)
        .find(eq("_id", pipelineId)).iterator().next();
  }

  public void createPipeline(Pipeline pipeline) {
    enhance(pipeline);
    // TODO: versioning, cache, exception if not found, etc
    pipelineRunConfig.getMongoDatabase().getCollection(pipelineRunConfig.getStoreCollection(), Pipeline.class)
        .insertOne(pipeline);
  }

  public void update(Pipeline pipeline) {
    // TODO: On each update increment Pipeline#version.
  }

  private void enhance(Pipeline pipeline) {
    if (pipeline.getPipeline() != null) {
      pipeline.setPipelineAsString(BsonUtil.toString(pipeline.getPipeline()));
    } else if (pipeline.getPipelineAsString() != null) {
      pipeline.setPipeline(toBsonDocumentList(pipeline.getPipelineAsString())); // Important: Store as native BsonDocument list in MongoDB and not as a String.
    }
  }
}
