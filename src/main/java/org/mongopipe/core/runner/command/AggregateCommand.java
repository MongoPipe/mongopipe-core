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

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import org.mongopipe.core.config.PipelineRunContext;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.param.AggregateParams;
import org.mongopipe.core.runner.evaluation.BsonParameterEvaluator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AggregateCommand implements MongoCommand {
  private final Pipeline pipeline;
  private final PipelineRunContext pipelineRunConfig;
  private final Map<String, ?> parameters;
  private final Class returnPojoClass;
  private final BsonParameterEvaluator bsonParameterEvaluator;

  public AggregateCommand(Pipeline pipeline, PipelineRunContext pipelineRunContext, Map<String, ?> parameters, Class returnPojoClass) {
    this.pipeline = pipeline;
    this.parameters = parameters;
    this.pipelineRunConfig = pipelineRunContext;
    this.returnPojoClass = returnPojoClass;
    this.bsonParameterEvaluator = new BsonParameterEvaluator(parameters);
  }

  @Override
  public Object run() {
    MongoCollection mongoCollection = pipelineRunConfig.getMongoDatabase().getCollection(pipeline.getCollection());
    AggregateParams aggregateParams = pipeline.getCommandAndParamsAs(AggregateParams.class);

    List actualPipeline = bsonParameterEvaluator.evaluate(pipeline.getPipeline());
    AggregateIterable aggregateIterable = mongoCollection.aggregate(actualPipeline, returnPojoClass);
    if (aggregateParams != null) {
      setAggregationOptions(aggregateIterable, aggregateParams);
    }
    return aggregateIterable;
  }

  private void setAggregationOptions(AggregateIterable aggregateIterable, AggregateParams aggregateParams) {
    aggregateIterable.allowDiskUse(aggregateParams.getAllowDiskUse());
    aggregateIterable.comment(aggregateParams.getComment());
    if (aggregateParams.getHint() != null) {
      aggregateIterable.hintString(aggregateParams.getHint());
    }
    if (aggregateParams.getBatchSize() != null) {
      aggregateIterable.batchSize(aggregateParams.getBatchSize());
    }
    if (aggregateParams.getBypassDocumentValidation() != null) {
      aggregateIterable.bypassDocumentValidation(aggregateParams.getBypassDocumentValidation());
    }
    if (aggregateParams.getExplainVerbosity() != null) {
      aggregateIterable.explain(aggregateParams.getExplainVerbosity());
    }
    if (aggregateParams.getMaxTimeMS() != null) {
      aggregateIterable.maxTime(aggregateParams.getMaxTimeMS(), TimeUnit.MILLISECONDS);
    }
    if (aggregateParams.getMaxAwaitTime() != null) {
      aggregateIterable.maxAwaitTime(aggregateParams.getMaxAwaitTime(), TimeUnit.MILLISECONDS);
    }
    org.mongopipe.core.runner.command.param.Collation collation = aggregateParams.getCollation();
    if (collation != null) {
      Collation mongoCollation = Collation.builder()
          .collationAlternate(collation.getAlternate())
          .collationCaseFirst(collation.getCaseFirst())
          .collationMaxVariable(collation.getMaxVariable())
          .collationStrength(collation.getStrength())
          .backwards(collation.getBackwards())
          .caseLevel(collation.getCaseLevel())
          .locale(collation.getLocale())
          .normalization(collation.getNormalization())
          .numericOrdering(collation.getNumericOrdering())
          .build();
      aggregateIterable.collation(mongoCollation);
    }
  }
}
