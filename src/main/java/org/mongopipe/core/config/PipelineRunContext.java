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

package org.mongopipe.core.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.migration.PipelineMigrationSource;

/**
 * Context keeps not only user provided configuration but also db connection, migration source, etc.
 */
@Data
public class PipelineRunContext {

  /**
   * The {@link MongoClient} can be also by the user accordingly.
   * This is more flexible since it allows you to configure your own connection parameters, codecs, etc.
   */
  private MongoClient mongoClient;
  private MongoDatabase mongoDatabase;

  private final PipelineRunConfig pipelineRunConfig;
  // The source implementation providing the pipelines for migration.
  private final PipelineMigrationSource pipelineMigrationSource;

  /**
   * Creates the connection and returns the database connection.
   * Called from PipelineStore or PipelineRunner.
   */
  public MongoDatabase getMongoDatabase() {
    if (mongoClient == null) { // If user did not provided MongoClient, create one here.
      if (pipelineRunConfig.getUri() == null) {
        throw new MongoPipeConfigException("URI can not be null. Alternatively you need to provide your own com.mongodb.client.MongoClient.");
      }
      ConnectionString connectionString = new ConnectionString(pipelineRunConfig.getUri());
      MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
      mongoClient = MongoClients.create(mongoClientSettings);
    }
    if (mongoDatabase == null) {
      // Alternatively you can set on the collection: collection.withCodecRegistry(pojoCodecRegistry)
      mongoDatabase = mongoClient.getDatabase(pipelineRunConfig.getDatabaseName()).withCodecRegistry(PojoCodecConfig.getCodecRegistry());
    }

    return mongoDatabase;
  }

  public MongoClient getMongoClient() {
    return mongoClient;
  }

  public void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public void setMongoDatabase(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  public PipelineRunConfig getPipelineRunConfig() {
    return pipelineRunConfig;
  }

  public PipelineMigrationSource getPipelineMigrationSource() {
    return pipelineMigrationSource;
  }
}
