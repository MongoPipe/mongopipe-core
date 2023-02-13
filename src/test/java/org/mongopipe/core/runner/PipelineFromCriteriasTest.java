/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mongopipe.core.runner;

import org.bson.conversions.Bson;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.model.Pizza;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.BsonUtil;
import org.mongopipe.core.util.Maps;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;

public class PipelineFromCriteriasTest extends AbstractMongoDBTest {

  @Test
  public void testCreationOfPipelineDynamicallyViaMongoApi() {
    // Given
    db.getCollection("testCollection").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));

    // Pipeline stages one by one.
    List<Bson> pipelineBson = Arrays.asList(
        // Static imports from com.mongodb.client.model.Aggregates
        match(and(eq("size", "${size}"), eq("available", "${available}"))),
        sort(descending("price")),
        limit(3)
    );
    Pipeline dynamicPipeline = Pipeline.builder()
        .id("dynamicPipeline")
        .pipeline(pipelineBson)
        .collection("testCollection")
        .build();

    // Create
    Stores.getPipelineStore().create(dynamicPipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Pizza> pizzas = pipelineRunner.runAndList(dynamicPipeline.getId(), Pizza.class,
        Maps.of("size", "medium", "available", true));

    // Then
    assertEquals(3, pizzas.size());
    assertEquals(Float.valueOf(20), pizzas.get(0).getPrice());
  }

  @Test
  public void testPipelineToStringFullCircle() {
    // Given
    // Pipeline stages one by one.
    List<Bson> pipelineBson = Arrays.asList(
        // Static imports from com.mongodb.client.model.Aggregates
        match(and(eq("size", "${size}"), eq("available", "${available}"))),
        sort(descending("price")),
        limit(3)
    );
    Pipeline dynamicPipeline = Pipeline.builder()
        .id("dynamicPipeline")
        .pipeline(pipelineBson)
        .collection("testCollection")
        .build();

    // When
    String pipelineAsString = dynamicPipeline.toString();

    Pipeline restoredFromStringPipeline = BsonUtil.toPojo(pipelineAsString, Pipeline.class);
    String restoredFromString = restoredFromStringPipeline.toString();
    assertEquals(pipelineAsString, restoredFromString);
  }

  @Test
  public void testPipelineWithStageContentProvidedAsAMap() {
    // This is useful when only specific subparts of the pipeline need to be constructed dynamically while you want to keep the rest static
    // in the DB pipeline store.

    // Given
    db.getCollection("testCollection").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));

    // This will normally come from an outside source, provided by an admin/DBA.
    String jsonString = "{ \"_id\": \"dynamicPipeline\", \"version\": 1, \"collection\": \"testCollection\", \"pipelineAsString\":\"[" +
        "{\\\"$match\\\": {\\\"$and\\\": [{\\\"size\\\": \\\"${size}\\\"}, {\\\"available\\\": \\\"${available}\\\"}]}}," +
        "{\\\"$sort\\\": \\\"${sortMap}\\\" }," +
        "{\\\"$limit\\\": 3}]\" }";
    Pipeline pipeline = BsonUtil.toPojo(jsonString, Pipeline.class);
    Stores.getPipelineStore().create(pipeline);


    PipelineRunner pipelineRunner = Pipelines.getRunner();
    List<Pizza> pizzas = pipelineRunner.runAndList(pipeline.getId(), Pizza.class,
        Maps.of("size", "medium", "available", true, "sortMap", Maps.of("price", -1, "name", 1)));

    // Then
    assertEquals(3, pizzas.size());
  }
}
