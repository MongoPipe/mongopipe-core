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

package org.mongopipe.core.config;

import com.mongodb.MongoClientSettings;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.mongopipe.core.runner.command.param.AggregateParams;
import org.mongopipe.core.runner.command.param.FindOneAndUpdateOptions;
import org.mongopipe.core.runner.command.param.UpdateManyOptions;
import org.mongopipe.core.runner.command.param.UpdateOneOptions;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PojoCodecConfig {

  private static CodecRegistry codecRegistry;

  /**
   * @return the CodecRegistry used internally. Can be enhanced by the user with new ClassModels if needed.
   */
  public static CodecRegistry getCodecRegistry() {
    if (codecRegistry != null) {
      return codecRegistry;
    }
    // Set POJO codec registry.
    // https://mongodb.github.io/mongo-java-driver/4.7/driver/getting-started/quick-start-pojo/
    // https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/pojo-customization/
    //    ClassModel<AggregateParams> classModel = ClassModel.builder(AggregateParams.class).
    //        conventions(Arrays.asList(Conventions.ANNOTATION_CONVENTION)).build();
    ClassModel<AggregateParams> aggregateParamsClassModel = ClassModel.builder(AggregateParams.class)
        .enableDiscriminator(true).build();
    ClassModel<UpdateOneOptions> updateOneParamsClassModel = ClassModel.builder(UpdateOneOptions.class)
        .enableDiscriminator(true).build();
    ClassModel<UpdateManyOptions> updateManyParamsClassModel = ClassModel.builder(UpdateManyOptions.class)
        .enableDiscriminator(true).build();
    ClassModel<FindOneAndUpdateOptions> findOneAndUpdateParamsClassModel = ClassModel.builder(FindOneAndUpdateOptions.class)
        .enableDiscriminator(true).build();


    codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder()
            .automatic(true)
            .register(aggregateParamsClassModel, updateOneParamsClassModel, updateManyParamsClassModel, findOneAndUpdateParamsClassModel)
            .build()));
    return codecRegistry;
  }
}
