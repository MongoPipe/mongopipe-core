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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONException;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.Pipeline;
import org.mongopipe.core.annotation.PipelineRepository;
import org.mongopipe.core.model.PipelineRun;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.Maps;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.mongopipe.core.util.BsonUtil.*;
import static org.mongopipe.core.util.JsonUtil.assertJsonEqual;
import static org.mongopipe.core.util.JsonUtil.convertPojoToJson;

public class PipelineRunTest extends AbstractMongoDBTest {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRunTest.class);



  @PipelineRepository  // Optional, TODO: unit test.
  public interface MyRestaurant {

    @Pipeline("pipelineOne")
    List<Document> runMyFirstPipeline(@Param("pizzaSize") String pizzaSize);  // TODO: Test with result Pojo class to check conversion, will need to be implemented.

    @Pipeline("pizzasBySize")
    List<Pizza> getMatchingPizzas(@Param("pizzaSize") String pizzaSize);

  }

  @Test
  public void testSimplePipelineRunWithInlinePipelineRun() {

    ConnectionString connectionString = new ConnectionString("mongodb://localhost:" + port);
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();
    MongoClient mongoClient = MongoClients.create(mongoClientSettings);
    MongoDatabase db = mongoClient.getDatabase("test");
    db.getCollection("testCollection").insertMany(loadResourcePathIntoDocumentList("runner/data.bson"));

    Pipelines.newConfig()
        .uri("mongodb://localhost:" + port)
        .databaseName("test")
        .storeCollection("pipelines_store")
        .repositoriesScanPackage("org.mongopipe")
        .build();


    // Create pipeline manually. Can be also created from a pipeline.bson file.
    Pipelines.getStore().createPipeline(PipelineRun.builder()
        .id("pipelineOne")
        .jsonPipeline("[\n" +     // Inline as JSON. You can also provide it as BSON as this is the way it is stored.
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


    // Without Spring, you need first to manually get the pipeline repository.
    List<Document> reports = Pipelines.from(MyRestaurant.class)
        .runMyFirstPipeline("medium");

    String expected = "[{\"_id\":\"Pepperoni\",\"totalQuantity\":20},{\"_id\":\"Cheese\",\"totalQuantity\":50},{\"_id\":\"Vegan\",\"totalQuantity\":10}]";
    assertJsonEqual(expected, reports);

    // And test also the manual way of running pipelines, without annotations.
    PipelineRunner pipelineRunner = Pipelines.getRunner();
    reports = pipelineRunner.run("pipelineOne", Document.class, Maps.paramsMap("pizzaSize", "medium")).collect(Collectors.toList());
    assertJsonEqual(expected, reports);
  }

  @Test
  public void testWithFileBasedPipeline() throws JSONException {
    ConnectionString connectionString = new ConnectionString("mongodb://localhost:" + port);
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .build();
    MongoClient mongoClient = MongoClients.create(mongoClientSettings);
    MongoDatabase db = mongoClient.getDatabase("test");

    Pipelines.newConfig()
        .uri("mongodb://localhost:" + port)
        .databaseName("test")
        .storeCollection("pipelines_store")
        .repositoriesScanPackage("org.mongopipe")
        .build();

    //Pizza pizza = loadResourcePathIntoBsonDocument("runner/pizza.bson", Pizza.class);
    PipelineRun pipelineRun = loadResourcePathIntoPojo("runner/pizzasBySize.bson", PipelineRun.class);
    db.getCollection(pipelineRun.getCollection()).insertMany(loadResourcePathIntoDocumentList("runner/data.bson"));

    // Create pipeline.
    Pipelines.getStore().createPipeline(pipelineRun);

    List<Pizza> pizzas = Pipelines.from(MyRestaurant.class)
        .getMatchingPizzas("medium");
    assertEquals(3, pizzas.size());
    assertTrue(pizzas.get(0) instanceof Pizza);
    String expected = "[{\"date\": {\"$date\": \"2021-03-13T09:13:24Z\"}, \"_id\": 1, \"name\": \"Pepperoni\"},{\"date\": {\"$date\": \"2022-01-12T21:23:13.331Z\"}, \"_id\": 4, \"name\": \"Cheese\"},{\"date\": {\"$date\": \"2021-01-13T05:10:13Z\"}, \"_id\": 7, \"name\": \"Vegan\"}]";
    JSONAssert.assertEquals(expected, convertPojoToJson(pizzas), false);
  }
}
