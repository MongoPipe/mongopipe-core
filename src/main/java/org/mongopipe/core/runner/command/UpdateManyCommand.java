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
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.mongopipe.core.config.PipelineRunContext;
import org.mongopipe.core.model.Pipeline;

import java.util.List;
import java.util.Map;

public class UpdateManyCommand extends UpdateOneCommand { // Add common base class in future.

  public UpdateManyCommand(Pipeline pipeline, PipelineRunContext pipelineRunContext, Map<String, ?> parameters, Class returnPojoClass) {
    super(pipeline, pipelineRunContext, parameters, returnPojoClass);
  }

  public UpdateResult run(MongoCollection mongoCollection, BsonDocument filter, List<Bson> actualPipeline,UpdateOptions updateOptions) {
    return mongoCollection.updateMany(filter, actualPipeline, updateOptions);
  }
}
