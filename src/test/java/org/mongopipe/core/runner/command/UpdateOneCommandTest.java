/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
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

package org.mongopipe.core.runner.command;

import org.bson.Document;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.param.UpdateOneOptions;
import org.mongopipe.core.store.MyRestaurant;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.BsonUtil;
import org.mongopipe.core.util.Maps;

import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;
import static org.mongopipe.core.util.BsonUtil.toDocument;

public class UpdateOneCommandTest extends AbstractMongoDBTest {

  @Test
  public void testCommandRunningWithParams() {
    // Given
    Document filter = toDocument("{'price': \"${pizzaPrice}\"}");
    Pipeline pipeline = loadResourceIntoPojo("command/updateOne/updateOneMatchingPizza.bson", Pipeline.class);
    pipeline.setCommandOptions(UpdateOneOptions.builder().filter(filter).build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);

    // When
    long count = Stores.from(MyRestaurant.class).updateOnePizzaByPizzaPrice(12);

    // Then
    assertEquals(1, count);
  }

  @Test
  public void testNumberParamGivenInsideStringIsStillReplacedWithANumber() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/updateOne/updateOneMatchingPizza.bson", Pipeline.class);
    // Although the parameter is enclosed in quotes(i.e. a String), because it
    // is a number and matches the full width of the string it will
    // be replaced with a number.
    pipeline.setCommandOptions(UpdateOneOptions.builder().filter(toDocument("{'price': \"${pizzaPrice}\"}")).build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);

    // When
    long count = Stores.from(MyRestaurant.class).updateOnePizzaByPizzaPrice(12);

    // Then
    assertEquals(1, count);
  }

  @Test
  public void testUpdatePipelineReallyWorks() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/updateOne/updateOneMatchingPizza.bson", Pipeline.class);
    // Although the parameter is enclosed in quotes(i.e. a String), because it
    // is a number and matches the full width of the string it will
    // be replaced with a number.
    pipeline.setCommandOptions(UpdateOneOptions.builder().filter(toDocument("{'price': \"${pizzaPrice}\"}")).build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);

    // When
    long count = Pipelines.getRunner().run("updateOneMatchingPizza", Maps.of("pizzaPrice", 12), Long.class);

    // Then
    assertEquals(1, count);
    // Test that pipeline added extra field.
    assertTrue(db.getCollection(pipeline.getCollection()).find(BsonUtil.toBsonDocument("price", 12)).iterator().next().get("isVegan", Boolean.class));
  }
}
