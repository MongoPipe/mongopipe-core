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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.param.BaseUpdateParams;
import org.mongopipe.core.runner.evaluation.BsonParameterEvaluator;

import java.util.List;
import java.util.Map;

import static org.mongopipe.core.util.BsonUtil.toBsonDocument;

/**
 * https://www.mongodb.com/docs/manual/reference/method/db.collection.updateOne/
 */
public class UpdateOneCommand implements MongoCommand {
  private final Pipeline pipeline;
  private final PipelineRunConfig pipelineRunConfig;
  private final Map<String, ?> parameters;
  private final Class returnPojoClass;
  BsonParameterEvaluator bsonParameterEvaluator;

  public UpdateOneCommand(Pipeline pipeline, PipelineRunConfig pipelineRunConfig, Map<String, ?> parameters, Class returnPojoClass) {
    this.pipeline = pipeline;
    this.pipelineRunConfig = pipelineRunConfig;
    this.parameters = parameters;
    this.returnPojoClass = returnPojoClass;
    this.bsonParameterEvaluator = new BsonParameterEvaluator(parameters);
  }

  public UpdateResult run(MongoCollection mongoCollection, BsonDocument filter, List<BsonDocument> actualPipeline,
      UpdateOptions updateOptions) {
    return mongoCollection.updateOne(filter, actualPipeline, updateOptions);
  }

  @Override
  public Object run() {
    MongoCollection mongoCollection = pipelineRunConfig.getMongoDatabase().getCollection(pipeline.getCollection());

    BaseUpdateParams baseUpdateParams = pipeline.getCommandAndParamsAs(BaseUpdateParams.class);
    BsonDocument filter = buildFilter(baseUpdateParams);
    List<BsonDocument> actualPipeline = bsonParameterEvaluator.evaluate(pipeline.getPipeline());

    UpdateOptions updateOptions = buildUpdateOptions(baseUpdateParams);
    UpdateResult updateResult = run(mongoCollection, filter, actualPipeline, updateOptions);

    if (Boolean.class.equals(returnPojoClass)) {
      return updateResult.wasAcknowledged();
    } else if (Long.class.equals(returnPojoClass) || Integer.class.equals(returnPojoClass)) {
      return updateResult.getModifiedCount();
    } else if (Boolean.class.equals(returnPojoClass)) {
      return updateResult;
    } else {
      throw new MongoPipeConfigException("Pipeline '" + pipeline.getId() + "' result can not be mapped to '"
          + returnPojoClass.getClass().getCanonicalName() + "'" +
          " Try returning an object of type Long, Boolean or " + UpdateResult.class.getCanonicalName());
    }
  }

  private BsonDocument buildFilter(BaseUpdateParams updateOneParams) {
    if (updateOneParams == null || updateOneParams.getFilter() == null) {
      throw new MongoPipeConfigException("At least 'filter' parameter needs to provided for the 'updateOne' command");
    }
    BsonDocument bsonDocument = toBsonDocument(updateOneParams.getFilter());
    bsonParameterEvaluator.evaluate(bsonDocument); // Evaluate also the filter
    return bsonDocument;
  }

  private UpdateOptions buildUpdateOptions(BaseUpdateParams baseUpdateParams) {
    UpdateOptions updateOptions = new UpdateOptions();
    updateOptions.arrayFilters(baseUpdateParams.getArrayFilters());
    updateOptions.hintString(baseUpdateParams.getHint());
    updateOptions.upsert(baseUpdateParams.getUpsert() != null ? baseUpdateParams.getUpsert() : false);
    updateOptions.bypassDocumentValidation(baseUpdateParams.getBypassDocumentValidation() != null ? baseUpdateParams.getBypassDocumentValidation() : false);
    updateOptions.comment(baseUpdateParams.getComment());
    updateOptions.let(baseUpdateParams.getLet());
    if (baseUpdateParams.getCollation() != null) {
      org.mongopipe.core.runner.command.param.Collation userCollation = baseUpdateParams.getCollation();
      updateOptions.collation(Collation.builder()
          .collationAlternate(userCollation.getAlternate())
          .collationCaseFirst(userCollation.getCaseFirst())
          .collationStrength(userCollation.getStrength())
          .collationMaxVariable(userCollation.getMaxVariable())
          .backwards(userCollation.getBackwards() != null ? userCollation.getBackwards() : false)
          .locale(userCollation.getLocale())
          .caseLevel(userCollation.getCaseLevel())
          .normalization(userCollation.getNormalization() != null ? userCollation.getNormalization() : false)
          .numericOrdering(userCollation.getNumericOrdering() != null ? userCollation.getNumericOrdering() : false)
          .build());
    }

    return updateOptions;
  }
}
