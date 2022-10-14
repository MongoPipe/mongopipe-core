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

package org.mongopipe.core;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Test;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.Pipeline;
import org.mongopipe.core.annotation.PipelineRepository;
import org.mongopipe.core.model.PipelineRun;
import org.mongopipe.core.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mongopipe.core.util.TestUtil.*;

public class TestPipeline extends AbstractMongoDBTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestPipeline.class);



  @PipelineRepository  // Optional, TODO: unit test.
  public interface MyRestaurant {

    @Pipeline("pipelineOne")
    List<Document> runMyFirstPipeline(@Param("pizzaSize") String pizzaSize);  // TODO: Test with result Pojo class to check conversion, will need to be implemented.
  }

  @Test
  public void testSimplePipelineRun() {

    ConnectionString connectionString = new ConnectionString("mongodb://localhost:" + port);
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();
    MongoClient mongoClient = MongoClients.create(mongoClientSettings);
    MongoDatabase db = mongoClient.getDatabase("test");
    db.getCollection("orders").insertMany(convertJsonArrayToDocumentList("[\n" +
        "        { _id: 0, name: \"Pepperoni\", size: \"small\", price: 19,\n" +
        "        quantity: 10, date: ISODate( \"2021-03-13T08:14:30Z\" ) },\n" +
        "    { _id: 1, name: \"Pepperoni\", size: \"medium\", price: 20,\n" +
        "        quantity: 20, date : ISODate( \"2021-03-13T09:13:24Z\" ) },\n" +
        "    { _id: 2, name: \"Pepperoni\", size: \"large\", price: 21,\n" +
        "        quantity: 30, date : ISODate( \"2021-03-17T09:22:12Z\" ) },\n" +
        "    { _id: 3, name: \"Cheese\", size: \"small\", price: 12,\n" +
        "        quantity: 15, date : ISODate( \"2021-03-13T11:21:39.736Z\" ) },\n" +
        "    { _id: 4, name: \"Cheese\", size: \"medium\", price: 13,\n" +
        "        quantity:50, date : ISODate( \"2022-01-12T21:23:13.331Z\" ) },\n" +
        "    { _id: 5, name: \"Cheese\", size: \"large\", price: 14,\n" +
        "        quantity: 10, date : ISODate( \"2022-01-12T05:08:13Z\" ) },\n" +
        "    { _id: 6, name: \"Vegan\", size: \"small\", price: 17,\n" +
        "        quantity: 10, date : ISODate( \"2021-01-13T05:08:13Z\" ) },\n" +
        "    { _id: 7, name: \"Vegan\", size: \"medium\", price: 18,\n" +
        "        quantity: 10, date : ISODate( \"2021-01-13T05:10:13Z\" ) }\n" +
        "]"));


    Pipelines.newConfig()
        .uri("mongodb://localhost:" + port)
        .databaseName("test")
        .storeCollection("pipelines_store")
        .repositoriesScanPackage("org.mongopipe")
        .build();

    Pipelines.getStore().createPipeline(PipelineRun.builder()
        .id("pipelineOne")
        .pipeline("[\n" +
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
        .collection("orders")
        .build());


    // Without Spring, you need first to manually get the pipeline repository.
    List<Document> reports = Pipelines.from(MyRestaurant.class)
        .runMyFirstPipeline("medium");

    String expected = "[{\"_id\":\"Pepperoni\",\"totalQuantity\":20},{\"_id\":\"Cheese\",\"totalQuantity\":50},{\"_id\":\"Vegan\",\"totalQuantity\":10}]";
    assertJsonEqual(expected, reports);

    // And test also the manual way of running pipelines, without annotations.
    PipelineRunner pipelineRunner = Pipelines.getRunner();
    reports = pipelineRunner.run("pipelineOne", List.class, Maps.paramsMap("pizzaSize", "medium"));
    assertJsonEqual(expected, reports);

  }

}
