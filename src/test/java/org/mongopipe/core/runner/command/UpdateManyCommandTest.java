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
import org.mongopipe.core.runner.command.param.UpdateManyParams;
import org.mongopipe.core.util.AbstractMongoDBTest;

import static org.mongopipe.core.util.BsonUtil.*;

public class UpdateManyCommandTest extends AbstractMongoDBTest {

  @Test
  public void testUpdateManyPipelineReallyWorks() {
    // Given
    Document filter = toDocument("{'size': \"medium\"}");
    Pipeline pipeline = loadResourceIntoPojo("command/updateMany/updateManyMatchingPizza.bson", Pipeline.class);
    // Although the parameter is enclosed in quotes(i.e. a String), because it is a number and matches the full width of the string it will
    // be replaced with a number.
    pipeline.setCommandAndParams(UpdateManyParams.builder()
        .filter(filter)
        .build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);

    // When
    long count = Pipelines.getRunner().run("updateManyMatchingPizza", filter, Long.class);

    // Then
    assertEquals(3, count);
    // Test that pipeline added extra field.
    assertTrue(db.getCollection(pipeline.getCollection())
        .find(filter).iterator().next().get("isVegan", Boolean.class));
  }
}
