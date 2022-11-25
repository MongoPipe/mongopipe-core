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

package org.mongopipe.core.runner.context;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.config.PojoCodecConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.migration.PipelineMigrationSource;
import org.mongopipe.core.runner.PipelineRunner;
import org.mongopipe.core.store.PipelineStore;

/**
 * Context keeps not only user provided configuration but also db connection, migration source, etc.
 */
public class RunContext {

  /**
   * The {@link MongoClient} can be also by the user accordingly.
   * This is more flexible since it allows you to configure your own connection parameters, codecs, etc.
   */
  protected String id;
  protected MongoClient mongoClient;
  protected MongoDatabase mongoDatabase;

  protected final MongoPipeConfig mongoPipeConfig;
  // The source implementation providing the pipelines for migration.
  protected PipelineMigrationSource pipelineMigrationSource;

  public RunContext(MongoPipeConfig mongoPipeConfig) {
    id = mongoPipeConfig.getId(); // same as the mongoPipeConfig
    this.mongoPipeConfig = mongoPipeConfig;
    this.mongoClient = mongoPipeConfig.getMongoClient(); // nullable
  }

  /**
   * Creates the connection and returns the database connection.
   * Called from PipelineStore or PipelineRunner.
   */
  public MongoDatabase getMongoDatabase() {
    // If user did not provided MongoClient, create one here.
    if (mongoClient == null) {
      if (mongoPipeConfig.getUri() == null) {
        throw new MongoPipeConfigException("URI can not be null. Alternatively you need to provide your own com.mongodb.client.MongoClient.");
      }
      ConnectionString connectionString = new ConnectionString(mongoPipeConfig.getUri());
      MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
      mongoClient = MongoClients.create(mongoClientSettings);
    }

    if (mongoDatabase == null) {
      if (mongoPipeConfig.getDatabaseName() == null) {
        throw new MongoPipeConfigException("Database name not provided in the configuration");
      }
      // Alternatively you can set on the collection: collection.withCodecRegistry(pojoCodecRegistry)
      mongoDatabase = mongoClient.getDatabase(mongoPipeConfig.getDatabaseName()).withCodecRegistry(PojoCodecConfig.getCodecRegistry());
    }

    return mongoDatabase;
  }

  public String getId() {
    return id;
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

  public MongoPipeConfig getMongoPipeConfig() {
    return mongoPipeConfig;
  }

  public PipelineMigrationSource getPipelineMigrationSource() {
    return pipelineMigrationSource;
  }

  public void setPipelineMigrationSource(PipelineMigrationSource pipelineMigrationSource) {
    this.pipelineMigrationSource = pipelineMigrationSource;
  }

  public PipelineRunner getRunner() {
    return Pipelines.getRunner(id);
  }

  public PipelineStore getStore() {
    return Stores.getPipelineStore();
  }


}
