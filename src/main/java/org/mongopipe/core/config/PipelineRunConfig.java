/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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
import org.mongopipe.core.exception.MongoPipeConfigException;

/**
 * Library main configuration. It establishes a Mongo connection unless MongoClient is provided.
 * NOTE: Create one config per Mongo database.
 */
public class PipelineRunConfig extends PipelineStoreConfig {
  /**
   * The {@link MongoClient} is configured by the user accordingly.
   */
  MongoClient mongoClient;
  private MongoDatabase mongoDatabase;

  private PipelineRunConfig(Builder builder) {
    mongoClient = builder.mongoClient;
    uri = builder.uri;
    databaseName = builder.databaseName;
    id = builder.id;
    pipelineInterfacesScanPackage = builder.pipelineInterfacesScanPackage;
    storeCollection = builder.storeCollection;
    storeCacheEnabled = builder.storeCacheEnabled;
    migrationStatusCollection = builder.migrationStatusCollection;
  }

  public static Builder builder() {
    return new Builder();
  }


  public MongoDatabase getMongoDatabase() {
    if (mongoClient == null) { // If user did not provided MongoClient, create one here.
      if (getUri() == null) {
        throw new MongoPipeConfigException("URI can not be null. Alternatively provide com.mongodb.client.MongoClient instance in this " +
            "class constructor.");
      }
      ConnectionString connectionString = new ConnectionString(getUri());
      MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
      mongoClient = MongoClients.create(mongoClientSettings);
    }
    mongoDatabase = mongoClient.getDatabase(getDatabaseName());
    return mongoDatabase;
  }

  public static class Builder {
    private String id;
    private MongoClient mongoClient;
    private String uri;
    private String databaseName;
    private String pipelineInterfacesScanPackage;
    private String storeCollection;
    private boolean storeCacheEnabled;
    private String migrationStatusCollection;

    public Builder() {
    }

    /**
     * Optional.
     * This is more flexible since it allows you to configure your own connection parameters, codecs, etc.
     **/
    public Builder mongoClient(MongoClient val) {
      mongoClient = val;
      return this;
    }

    public Builder uri(String val) {
      uri = val;
      return this;
    }

    public Builder databaseName(String val) {
      databaseName = val;
      return this;
    }

    // The unique id identifying the configuration. Needs provided if using multiple configurations.
    public Builder id(String val) {
      id = val;
      return this;
    }

    public Builder repositoriesScanPackage(String val) {
      pipelineInterfacesScanPackage = val;
      return this;
    }

    public Builder storeCollection(String val) {
      storeCollection = val;
      return this;
    }

    public Builder storeCacheEnabled(boolean val) {
      storeCacheEnabled = val;
      return this;
    }

    public Builder migrationStatusCollection(String val) {
      migrationStatusCollection = val;
      return this;
    }

    public PipelineRunConfig build() {
      return new PipelineRunConfig(this);
    }
  }
}
