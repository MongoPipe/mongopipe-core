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
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.repository.MyRestaurant;
import org.mongopipe.core.runner.command.param.FindOneAndUpdateParams;
import org.mongopipe.core.util.AbstractMongoDBTest;

import static org.mongopipe.core.util.BsonUtil.*;

public class FindOneAndUpdateCommandTest extends AbstractMongoDBTest {

  @Test
  public void testCommandRunningWithParams() {
    // Given
    Document filter = toDocument("{'price': $pizzaPrice}");
    Pipeline pipeline = loadResourceIntoPojo("command/findOneAndUpdate/updateOneMatchingPizza.bson", Pipeline.class);
    pipeline.setCommandAndParams(FindOneAndUpdateParams.builder()
        .filter(filter)
        .returnNewDocument(true) // to return updated document and not the previous one.
        .upsert(true)
        .build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);

    // When
    Document pizza = Pipelines.from(MyRestaurant.class).findOneAndUpdate(12);

    // Then
    assertTrue(pizza.get("isVegan", Boolean.class));
  }

}
