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
import org.mongopipe.core.migration.model.Status;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.param.CommandOptions;
import org.mongopipe.core.runner.command.param.FindOneAndUpdateOptions;
import org.mongopipe.core.store.MyRestaurant;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.mongopipe.core.util.Maps;

import static com.mongodb.client.model.Filters.eq;
import static org.mongopipe.core.util.BsonUtil.*;

public class FindOneAndUpdateCommandTest extends AbstractMongoDBTest {

  @Test
  public void testCommandRunningWithParams() {
    // Given
    Document filter = toDocument("{'price': \"${pizzaPrice}\"}");
    Pipeline pipeline = loadResourceIntoPojo("command/findOneAndUpdate/updateOneMatchingPizza.bson", Pipeline.class);
    pipeline.setCommandOptions(FindOneAndUpdateOptions.builder()
        .filter(filter)
        .returnNewDocument(true) // to return updated document and not the previous one.
        .upsert(true)
        .build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Stores.getPipelineStore().create(pipeline);

    // When
    Document pizza = Stores.get(MyRestaurant.class).findOneAndUpdate(12);

    // Then
    assertTrue(pizza.get("isVegan", Boolean.class));
  }

  @Test
  public void testUpsertWithAPipelineThatWasNotPreviouslySaved() {
    // Given
    Pipeline pipeline = Pipeline.builder()
        .id("1")
        .collection("pipeline_config")
        .pipeline("[{\"$set\": {\"version\": 2}}]")
        .commandOptions(CommandOptions.findOneAndUpdate()
            .filter(Document.parse("{'_id': 1}")) // or eq("_id", 1)
            .upsert(true)
            .returnNewDocument(true)
            // .updateDocument(Document.parse("{\"stringValue\": \"a\"}"))
            .build())
        .build();

    // When
    Stores.getPipelineStore().create(pipeline);
    Status mongoPipeConfig = Pipelines.getRunner().run("1", Status.class);

    // Then
    assertEquals(Long.valueOf(2), mongoPipeConfig.getVersion());
  }

  @Test
  public void testUpsertWithAPipelinePojoParameters() {
    // Given
    Pipeline pipeline = Pipeline.builder()
        .collection("pipeline_config")
        .pipeline("[{\"$set\": \"${config}\"}]")
        .commandOptions(CommandOptions.findOneAndUpdate()
            .filter(eq("_id", 1L)) // or Document.parse("{'_id': '1'}"))
            .upsert(true)
            .returnNewDocument(true)
            .build())
        .build();
    Status mongoConfig = Status.builder()
        .version(10L)
        .build();

    // When
    Status mongoPipeConfig = Pipelines.getRunner().run(pipeline, Status.class, Maps.of("config", mongoConfig));

    // Then
    assertEquals(Long.valueOf(10), mongoPipeConfig.getVersion());
    assertEquals(Long.valueOf(1), mongoPipeConfig.getId());
  }


}
