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

import com.mongodb.ExplainVerbosity;
import com.mongodb.MongoCommandException;
import org.json.JSONException;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.repository.MyRestaurant;
import org.mongopipe.core.runner.Pizza;
import org.mongopipe.core.runner.command.param.AggregateParams;
import org.mongopipe.core.runner.command.param.Collation;
import org.mongopipe.core.util.AbstractMongoDBTest;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;
import static org.mongopipe.core.util.TestUtil.convertPojoToJson;
import static org.mongopipe.core.util.TestUtil.getClasspathFileContent;

public class AggregateCommandTest extends AbstractMongoDBTest {

  @Test
  public void testParamsSaving() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);

    AggregateParams params = AggregateParams.builder()
        .explainVerbosity(ExplainVerbosity.QUERY_PLANNER)
        .batchSize(10)
        .allowDiskUse(true)
        .bypassDocumentValidation(true)
        .hint("hint")
        .comment("comment")
        .maxTimeMS(1L)
        .maxAwaitTime(2L)
        .collation(Collation.builder()
            .caseLevel(true)
            .locale("EN")
            .build())
        .build();

    pipeline.setCommandAndParams(params);

    // When
    Pipelines.getStore().createPipeline(pipeline);

    // Then
    AggregateParams aggregateParams = Pipelines.getStore().getPipeline(pipeline.getId()).getCommandAndParamsAs(AggregateParams.class);
    assertEquals("aggregate", aggregateParams.getType());
    assertEquals(params.getBatchSize(), aggregateParams.getBatchSize());
    assertEquals(params.getExplainVerbosity(), aggregateParams.getExplainVerbosity());
    assertEquals(params.getAllowDiskUse(), aggregateParams.getAllowDiskUse());
    assertEquals(params.getBypassDocumentValidation(), aggregateParams.getBypassDocumentValidation());
    assertEquals(params.getComment(), aggregateParams.getComment());
    assertEquals(params.getHint(), aggregateParams.getHint());
    assertEquals(params.getComment(), aggregateParams.getComment());
    assertEquals(params.getMaxTimeMS(), aggregateParams.getMaxTimeMS());
    assertEquals(params.getMaxAwaitTime(), aggregateParams.getMaxAwaitTime());
    assertEquals(params.getCollation().getLocale(), aggregateParams.getCollation().getLocale());
    assertEquals(params.getCollation().getCaseLevel(), aggregateParams.getCollation().getCaseLevel());
  }

  @Test
  public void testCommandRunningWithParams() throws JSONException {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    pipeline.setCommandAndParams(AggregateParams.builder()
        .explainVerbosity(ExplainVerbosity.QUERY_PLANNER)
        .batchSize(10)
        .allowDiskUse(true)
        .bypassDocumentValidation(true)
        //.hint("hint") TODO: Add test failing because of this, by this testing that params are really sent.
        .comment("comment")
        .maxTimeMS(10000L)
        .maxAwaitTime(200000L)
        .build());
    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);

    // When
    List<Pizza> pizzas = Pipelines.from(MyRestaurant.class).getMatchingPizzas("medium");

    // Then
    assertEquals(3, pizzas.size());
    assertTrue(pizzas.get(0) instanceof Pizza);
    JSONAssert.assertEquals(getClasspathFileContent("runner/pipelineRun/matchingPizzasBySize.result.json"), convertPojoToJson(pizzas), false);
  }

  @Test
  public void testCommandRunningWithFloatParamAndNonQuotedParameter() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/aggregate/pipelinePizzasByPrice.bson", Pipeline.class);

    db.getCollection(pipeline.getCollection()).insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
    Pipelines.getStore().createPipeline(pipeline);

    // When
    List<Pizza> pizzas = Pipelines.from(MyRestaurant.class).getMatchingPizzasByPrice(14.2);

    // Then
    assertEquals(1, pizzas.size());
  }

  @Test(expected = MongoCommandException.class)
  public void testFailingParameterValue() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("runner/pipelineRun/matchingPizzasBySize.pipeline.bson", Pipeline.class);
    pipeline.setCommandAndParams(AggregateParams.builder()
        .hint("hint")
        .build());
    Pipelines.getStore().createPipeline(pipeline);

    // When
    List<Pizza> pizzas = Pipelines.from(MyRestaurant.class).getMatchingPizzas("medium");
  }

  @Test
  public void testSavingForPipelineAndCommandParamsLoadedFromFile() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/aggregate/pipeline.bson", Pipeline.class);
    AggregateParams params = pipeline.getCommandAndParamsAs(AggregateParams.class);
    // When
    Pipelines.getStore().createPipeline(pipeline);
    // Then
    AggregateParams aggregateParams = Pipelines.getStore().getPipeline(pipeline.getId()).getCommandAndParamsAs(AggregateParams.class);
    assertEquals("aggregate", aggregateParams.getType());
    assertEquals(params.getBatchSize(), aggregateParams.getBatchSize());
    assertEquals(params.getExplainVerbosity(), aggregateParams.getExplainVerbosity());
    assertEquals(params.getAllowDiskUse(), aggregateParams.getAllowDiskUse());
    assertEquals(params.getBypassDocumentValidation(), aggregateParams.getBypassDocumentValidation());
    assertEquals(params.getComment(), aggregateParams.getComment());
    assertEquals(params.getHint(), aggregateParams.getHint());
    assertEquals(params.getComment(), aggregateParams.getComment());
    assertEquals(params.getMaxTimeMS(), aggregateParams.getMaxTimeMS());
    assertEquals(params.getMaxAwaitTime(), aggregateParams.getMaxAwaitTime());
    assertEquals(params.getCollation().getLocale(), aggregateParams.getCollation().getLocale());
    assertEquals(params.getCollation().getCaseLevel(), aggregateParams.getCollation().getCaseLevel());
  }
}
