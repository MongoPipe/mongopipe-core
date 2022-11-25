/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongopipe.core.runner.command.param;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

/**
 * Common for all Mongo commands using a pipeline as one of their parameters.
 * Check all classes implementing this interface for a set of supported commands.
 * Beside pipeline, the other parameters of the command are provided by a implementation of this class.
 */
@BsonDiscriminator(value="genericCommand", key="type")
public abstract class CommandOptions {
  public static final String TYPE_KEY = "type";

  public abstract String getType();

  public static AggregateParams.Builder aggregate() {
    return AggregateParams.builder();
  }
  public static FindOneAndUpdateOptions.Builder findOneAndUpdate() {
    return FindOneAndUpdateOptions.builder();
  }
  public static UpdateOneOptions.Builder updateOne() {
    return UpdateOneOptions.builder();
  }
  public static UpdateManyOptions.Builder updateMany() {
    return UpdateManyOptions.builder();
  }

}
