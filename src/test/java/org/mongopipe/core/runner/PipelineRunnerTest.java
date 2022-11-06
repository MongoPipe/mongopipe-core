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

import org.bson.Document;
import org.json.JSONException;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.PipelineRepository;
import org.mongopipe.core.annotation.PipelineRun;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.Maps;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;
import static org.mongopipe.core.util.TestUtil.*;

public class PipelineRunnerTest extends AbstractMongoDBTest {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRunnerTest.class);


  @PipelineRepository  // Optional, TODO: unit test.
  public interface MyRestaurant {

    @PipelineRun("pipelineOne")
    List<Document> runMyFirstPipeline(@Param("pizzaSize") String pizzaSize);  // TODO: Test with result Pojo class to check conversion, will need to be implemented.

    @PipelineRun("matchingPizzasBySize")
    List<Pizza> getMatchingPizzas(@Param("pizzaSize") String pizzaSize);

  }


  public void newPipelinesConfig(String storeCollection, boolean cacheEnabled) {
    // Consider this helper versus @Before because it allows configuration.
    Pipelines.newConfig()
            .uri("mongodb://localhost:" + PORT)
            .databaseName("test")
            .storeCollection(storeCollection)
            .repositoriesScanPackage("org.mongopipe")
            .storeCacheEnabled(cacheEnabled)
            .build();
  }

  @Test
  public void testSimplePipelineRunWithInlinePipelineRun() {
    // Given
    db.getCollection("testCollection").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    // Create pipeline manually. Can be also created from a pipeline.bson file.
    Pipelines.getStore().createPipeline(Pipeline.builder()
        .id("pipelineOne")
        .pipeline("[\n" +     // Inline as JSON but will be converted to BSON in the builder.
            "  {\n" +
            "    $match: {\n" +
            "      size: \"${pizzaSize}\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    $group: {\n" +
            "      _id: \"$name\",\n" +
            "      totalQuantity: {\n" +
            "        $sum: \"$quantity\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "]")
        .collection("testCollection")
        .build());

    // When
    // Without Spring, you need first to manually get the pipeline repository.
    List<Document> reports = Pipelines.from(MyRestaurant.class)
        .runMyFirstPipeline("medium");

    // Then
    String expected = "[{\"_id\":\"Pepperoni\",\"totalQuantity\":20},{\"_id\":\"Cheese\",\"totalQuantity\":50},{\"_id\":\"Vegan\",\"totalQuantity\":10}]";
    assertJsonEqual(expected, reports);
  }

  @Test
  public void testRunnerDirectlyWithoutAnnotations() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Document> reports = pipelineRunner.run("matchingPizzasBySize", Document.class, Maps.of("pizzaSize", "medium")).collect(Collectors.toList());

    // Then
    assertJsonEqual(getClasspathFileContent("runner/pipelineRun/testRunnerDirectlyWithoutAnnotations.result.json"), reports);
  }

  @Test
  public void testRunnerWithoutAnnotationsWithStoreCacheEnabled() {
    // Given
    newPipelinesConfig("pipelines_store", true);
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // When
    List<Document> reports = pipelineRunner.run("matchingPizzasBySize", Document.class, Maps.of("pizzaSize", "medium")).collect(Collectors.toList());

    // Then
    assertJsonEqual(getClasspathFileContent("runner/pipelineRun/testRunnerDirectlyWithoutAnnotations.result.json"), reports);
  }

  @Test
  public void testWithPojoClassForResult() throws JSONException {
    // Given
    Pipeline pipelineRun = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    db.getCollection(pipelineRun.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));

    Pipelines.getStore().createPipeline(pipelineRun);

    // When
    List<Pizza> pizzas = Pipelines.from(MyRestaurant.class).getMatchingPizzas("medium");

    // Then
    assertEquals(3, pizzas.size());
    assertTrue(pizzas.get(0) instanceof Pizza);
    JSONAssert.assertEquals(getClasspathFileContent("runner/pipelineRun/matchingPizzasBySize.result.json"), convertPojoToJson(pizzas), false);
  }

}
