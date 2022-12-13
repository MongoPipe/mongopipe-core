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

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static java.util.Arrays.asList;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;
import static org.mongopipe.core.util.TestUtil.*;

import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONException;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.model.Pizza;
import org.mongopipe.core.store.MyRestaurant;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.Maps;
import org.skyscreamer.jsonassert.JSONAssert;

public class PipelineRunnerTest extends AbstractMongoDBTest {

  public void newPipelinesConfig(String storeCollection, boolean cacheEnabled) {
    // Consider this helper versus @Before because it allows configuration.
    Stores.registerConfig(
        MongoPipeConfig.builder()
            .uri("mongodb://localhost:" + PORT)
            .databaseName("test")
            .storeCollection(storeCollection)
            .storeCacheEnabled(cacheEnabled)
            .build());
  }

  @Test
  public void testSimplePipelineWithNonAnnotatedParams() {
    // Given
    db.getCollection("testCollection").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    // Create pipeline manually. Can be also created from a pipeline.bson file.
    Stores.getPipelineStore()
        .create(
            Pipeline.builder()
                .id("pipelineOne")
                .pipeline(
                    "[\n"
                        + // Inline as JSON but will be converted to BSON in the
                        // builder.
                        "  {\n"
                        + "    $match: {\n"
                        + "      size: \"${pizzaSize}\"\n"
                        + "    }\n"
                        + "  },\n"
                        + "  {\n"
                        + "    $group: {\n"
                        + "      _id: \"$name\",\n"
                        + "      totalQuantity: {\n"
                        + "        $sum: \"$quantity\"\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "]")
                .collection("testCollection")
                .build());

    // When
    // Without Spring, you need first to manually get the pipeline store.
    List<Document> reports = Stores.from(MyRestaurant.class).runMyFirstPipeline("medium");

    // Then
    String expected = "[{\"_id\":\"Pepperoni\",\"totalQuantity\":20},{\"_id\":\"Cheese\",\"totalQuantity\":50},{\"_id\":\"Vegan\",\"totalQuantity\":10}]";
    assertJsonEqual(expected, reports);
  }

  @Test
  public void testRunnerDirectlyWithoutAnnotations() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Document> reports = pipelineRunner.run("matchingPizzasBySize", List.class, Maps.of("pizzaSize", "medium"));

    // Then
    assertJsonEqual(getClasspathFileContent("runner/pipelineRun/testRunnerDirectlyWithoutAnnotations.result.json"), reports);
  }

  @Test
  public void testTypeConversion() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Pizza> reports = pipelineRunner.runAndList("matchingPizzasBySize", Pizza.class, Maps.of("pizzaSize", "medium"));

    // Then
    assertEquals(Pizza.class, reports.get(0).getClass());
  }

  @Test
  public void testRunnerWithoutAnnotationsWithStoreCacheEnabled() {
    // Given
    newPipelinesConfig("pipeline_store", true);
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Document> reports = pipelineRunner.run("matchingPizzasBySize", List.class, Maps.of("pizzaSize", "medium"));

    // Then
    assertJsonEqual(getClasspathFileContent("runner/pipelineRun/testRunnerDirectlyWithoutAnnotations.result.json"), reports);
  }

  @Test
  public void testWithPojoClassForResultAndWithoutPipelineRunAnnotation() throws JSONException {
    // Given
    Pipeline pipelineRun = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySizeWhenMissingAnnotation.pipeline.bson", Pipeline.class);
    db.getCollection(pipelineRun.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));

    Stores.getPipelineStore().create(pipelineRun);

    // When
    List<Pizza> pizzas = Stores.from(MyRestaurant.class).matchingPizzasBySize("medium");

    // Then
    assertEquals(3, pizzas.size());
    assertTrue(pizzas.get(0) instanceof Pizza);
    JSONAssert.assertEquals(getClasspathFileContent("runner/pipelineRun/matchingPizzasBySize.result.json"), convertPojoToJson(pizzas), false);
  }

  @Test
  public void testCreationOfPipelineDynamicallyViaMongoApi() {
    // Given
    db.getCollection("testCollection").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Bson matchStage = match(and(eq("size", "${size}"), eq("available", "${available}"))); // Static imports from
    // com.mongodb.client.model.Aggregates
    Bson sortByCountStage = sort(descending("price"));
    Stores.getPipelineStore()
        .create(Pipeline.builder().id("dynamicPipeline").pipeline(asList(matchStage, sortByCountStage)).collection("testCollection").build());
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Pizza> pizzas = pipelineRunner.runAndList("dynamicPipeline", Pizza.class, Maps.of("size", "medium", "available", true));

    // Then
    assertEquals(3, pizzas.size());
    assertEquals(Float.valueOf(20), pizzas.get(0).getPrice());
  }
}
