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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.param.BaseFindUpdateParams;
import org.mongopipe.core.runner.evaluation.BsonParameterEvaluator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mongopipe.core.util.BsonUtil.toBsonDocument;

/**
 * https://www.mongodb.com/docs/manual/reference/method/db.collection.findOneAndUpdate
 */
public class FindOneAndUpdateCommand implements MongoCommand {
  private final Pipeline pipeline;
  private final RunContext runContext;
  private final Map<String, ?> parameters;
  private final Class returnPojoClass;
  BsonParameterEvaluator bsonParameterEvaluator;

  public FindOneAndUpdateCommand(Pipeline pipeline, RunContext runContext, Map<String, ?> parameters, Class returnPojoClass) {
    this.pipeline = pipeline;
    this.runContext = runContext;
    this.parameters = parameters;
    this.returnPojoClass = returnPojoClass;
    this.bsonParameterEvaluator = new BsonParameterEvaluator(parameters);
  }

  public Object run(MongoCollection mongoCollection, Bson filter, List<Bson> pipeline, FindOneAndUpdateOptions updateOptions) {
    return mongoCollection.findOneAndUpdate(filter, pipeline, updateOptions);
  }
  public Object run(MongoCollection mongoCollection, Bson filter, Bson updateDocument, FindOneAndUpdateOptions updateOptions) {
    return mongoCollection.findOneAndUpdate(filter, updateDocument, updateOptions);
  }


  @Override
  public Object run() {
    MongoCollection mongoCollection = runContext.getMongoDatabase().getCollection(pipeline.getCollection());

    BaseFindUpdateParams baseFindUpdateParams = pipeline.getCommandOptionsAs(BaseFindUpdateParams.class);
    BsonDocument filter = buildFilter(baseFindUpdateParams);

    FindOneAndUpdateOptions updateOptions = buildUpdateOptions(baseFindUpdateParams);
    Object updateResult;
    if (baseFindUpdateParams.getUpdateDocument() != null) {
      updateResult = run(mongoCollection, filter, baseFindUpdateParams.getUpdateDocument(), updateOptions);
    } else {
      List actualPipeline = bsonParameterEvaluator.evaluate(pipeline.getPipeline());
      updateResult = run(mongoCollection, filter, actualPipeline, updateOptions);
    }

    return updateResult;
  }

  private BsonDocument buildFilter(BaseFindUpdateParams baseFindUpdateParams) {
    if (baseFindUpdateParams == null || baseFindUpdateParams.getFilter() == null) {
      throw new MongoPipeConfigException("At least 'filter' parameter needs to provided for the 'updateOne' command");
    }
    BsonDocument bsonDocument = toBsonDocument(baseFindUpdateParams.getFilter());
    bsonParameterEvaluator.evaluate(bsonDocument); // Evaluate also the filter
    return bsonDocument;
  }

  private FindOneAndUpdateOptions buildUpdateOptions(BaseFindUpdateParams findAndUpdateParams) {
    FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
    findOneAndUpdateOptions.returnDocument(
        Boolean.TRUE.equals(findAndUpdateParams.getReturnNewDocument()) ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
    if (findAndUpdateParams.getMaxTimeMS() != null) {
      findOneAndUpdateOptions.maxTime(findAndUpdateParams.getMaxTimeMS(), TimeUnit.MILLISECONDS);
    }
    findOneAndUpdateOptions.sort(findAndUpdateParams.getSort());
    findOneAndUpdateOptions.upsert(findAndUpdateParams.getUpsert() != null ? findAndUpdateParams.getUpsert() : false);
    if (findAndUpdateParams.getCollation() != null) {
      org.mongopipe.core.runner.command.param.Collation userCollation = findAndUpdateParams.getCollation();
      findOneAndUpdateOptions.collation(Collation.builder()
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

    return findOneAndUpdateOptions;
  }
}
