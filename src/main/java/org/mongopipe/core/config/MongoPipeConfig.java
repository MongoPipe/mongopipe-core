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

import com.mongodb.client.MongoClient;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.runner.context.RunContextProvider;

/**
 * Library main configuration.
 * NOTE: Multiple configurations can coexist with different "id" field allowing the execution of pipelines on multiple databases.
 */
public class MongoPipeConfig {
  public static final String DEFAULT_STORE_COLLECTION = "pipeline_store";
  public static final String DEFAULT_HISTORY_COLLECTION = "pipeline_store_history";
  public static final String DEFAULT_STATUS_COLLECTION = "pipeline_status";

  /**
   * Connection string URI for the Mongo database.
   * NOTE: For custom database connection provide your own MongoClient to
   * {@link RunContext#setMongoClient}
   */
  protected String uri;

  /**
   * As alternative to providing the "uri", provide a MongoClient (including using certificates and other options).
   */
  protected MongoClient mongoClient;

  /**
   * Name of database on which pipelines are run. Required.
   */
  protected String databaseName;

  /**
   * If having multiple databases then you will need to provide an "id" identifying each.
   */
  protected String id = RunContextProvider.DEFAULT_CONTEXT_ID;

  /**
   * Name of collection storing pipelines.
   */
  protected String storeCollection = DEFAULT_STORE_COLLECTION;

  /**
   * If "true" when a pipeline is updated or deleted the old pipeline is moved into the pipeline_store_history collection.
   */
  protected boolean storeHistoryEnabled;

  protected String storeHistoryCollection = DEFAULT_HISTORY_COLLECTION;

  protected String statusCollection = DEFAULT_STATUS_COLLECTION;
  /**
   * If true then store provider should use local caching of the pipelines instead of hitting the database each time.
   * By default is disabled meaning it will read from the database the pipeline before each execution.
   * To manually refresh the cache like when the database is updated by another external process, manually call PipelineStore#refresh().
   */
  protected boolean storeCacheEnabled;

  protected MigrationConfig migrationConfig;

  /**
   * Scan package where to look for stores (@Store annotated). If not provided entire classpath will be scanned.
   */
  protected String scanPackage;

  private MongoPipeConfig(Builder builder) {
    setUri(builder.uri);
    setMongoClient(builder.mongoClient);
    setDatabaseName(builder.databaseName);
    setId(builder.id);
    setStoreCollection(builder.storeCollection);
    setStoreHistoryEnabled(builder.storeHistoryEnabled);
    setStoreHistoryCollection(builder.storeHistoryCollection);
    setStatusCollection(builder.statusCollection);
    setStoreCacheEnabled(builder.storeCacheEnabled);
    setMigrationConfig(builder.migrationConfig);
    setScanPackage(builder.scanPackage);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public MongoClient getMongoClient() {
    return mongoClient;
  }

  public void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStoreCollection() {
    return storeCollection;
  }

  public void setStoreCollection(String storeCollection) {
    this.storeCollection = storeCollection;
  }

  public boolean isStoreHistoryEnabled() {
    return storeHistoryEnabled;
  }

  public void setStoreHistoryEnabled(boolean storeHistoryEnabled) {
    this.storeHistoryEnabled = storeHistoryEnabled;
  }

  public String getStoreHistoryCollection() {
    return storeHistoryCollection;
  }

  public void setStoreHistoryCollection(String storeHistoryCollection) {
    this.storeHistoryCollection = storeHistoryCollection;
  }

  public String getStatusCollection() {
    return statusCollection;
  }

  public void setStatusCollection(String statusCollection) {
    this.statusCollection = statusCollection;
  }

  public boolean isStoreCacheEnabled() {
    return storeCacheEnabled;
  }

  public void setStoreCacheEnabled(boolean storeCacheEnabled) {
    this.storeCacheEnabled = storeCacheEnabled;
  }

  public MigrationConfig getMigrationConfig() {
    return migrationConfig;
  }

  public void setMigrationConfig(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  public String getScanPackage() {
    return scanPackage;
  }

  public void setScanPackage(String scanPackage) {
    this.scanPackage = scanPackage;
  }

  public static final class Builder {
    private String uri;
    private MongoClient mongoClient;
    private String databaseName;
    private String id;
    private String storeCollection = DEFAULT_STORE_COLLECTION;
    private boolean storeHistoryEnabled = true;
    private String storeHistoryCollection = DEFAULT_HISTORY_COLLECTION;
    private String statusCollection = DEFAULT_STATUS_COLLECTION;
    private boolean storeCacheEnabled;
    private MigrationConfig migrationConfig = MigrationConfig.builder().build();
    private String scanPackage;

    private Builder() {
    }

    public Builder uri(String val) {
      uri = val;
      return this;
    }

    public Builder mongoClient(MongoClient val) {
      mongoClient = val;
      return this;
    }

    public Builder databaseName(String val) {
      databaseName = val;
      return this;
    }

    public Builder id(String val) {
      id = val;
      return this;
    }

    public Builder storeCollection(String val) {
      storeCollection = val;
      return this;
    }

    public Builder storeHistoryEnabled(boolean val) {
      storeHistoryEnabled = val;
      return this;
    }

    public Builder storeHistoryCollection(String val) {
      storeHistoryCollection = val;
      return this;
    }

    public Builder statusCollection(String val) {
      statusCollection = val;
      return this;
    }

    public Builder storeCacheEnabled(boolean val) {
      storeCacheEnabled = val;
      return this;
    }

    public Builder migrationConfig(MigrationConfig val) {
      migrationConfig = val;
      return this;
    }

    public Builder scanPackage(String val) {
      scanPackage = val;
      return this;
    }

    public MongoPipeConfig build() {
      return new MongoPipeConfig(this);
    }
  }
}