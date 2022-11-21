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

import org.junit.Test;
import org.mongopipe.core.Stores;
import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.util.AbstractMongoDBTest;

import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;

public class PipelineHistoryStoreTest extends AbstractMongoDBTest {

  @Test
  public void testUpdatedAndThenDeletedPipelineIsSavedInHistory() {
    // Given
    Stores.registerConfig(MongoPipeConfig.builder()
        .uri("mongodb://localhost:" + PORT)
        .databaseName("test")
        .storeHistoryEnabled(true)
        .build());
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    Stores.getPipelineStore().create(pipeline);
    String oldDescription = pipeline.getDescription();
    String newDescription = "Changed description";

    // When
    pipeline.setDescription(newDescription);
    Stores.getPipelineStore().update(pipeline);

    // Then
    PipelineHistoryStore historyStore = Stores.get(PipelineHistoryStore.class);
    assertEquals(Long.valueOf(1L), historyStore.count());
    //List<Pipeline> oldPipelines = StreamSupport.stream(historyStore.findAll().spliterator(), false).collect(Collectors.toList());
    Pipeline oldPipeline = historyStore.findById(pipeline.getId());
    assertEquals(Long.valueOf(1), oldPipeline.getVersion());
    assertEquals(oldDescription, oldPipeline.getDescription());

    // Check after delete also.
    Stores.getPipelineStore().delete(pipeline);
    assertEquals(Long.valueOf(1L), historyStore.count());
    oldPipeline = historyStore.findById(pipeline.getId());
    assertEquals(Long.valueOf(2), oldPipeline.getVersion());
    assertEquals(newDescription, oldPipeline.getDescription());
  }

}
