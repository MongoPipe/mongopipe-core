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

import com.mongodb.client.MongoClient;

/**
 * POJO class used for configuring the library.
 * NOTE: Create one config per Mongo database.
 * In future this could be split this in 3 configuration classes (runner, store and org.mongopipe.core.migration).
 */
public class PipelineStoreConfig {
  /**
   * Connection string URI for the Mongo database.
   * NOTE: For custom database connection provide your own MongoClient instance to the constructor {@link PipelineRunConfig#setMongoClient(MongoClient)}
   */
  protected String uri;
  /**
   * Name of database on which pipelines are run. Required.
   */
  protected String databaseName;

  /**
   * If having multiple databases then you will need to provide an "id" identifying each.
   */
  protected String id;

  /**
   * The package where to look for interfaces with @Pipeline annotations.
   */
  protected String pipelineInterfacesScanPackage;

  /**
   * Name of collection storing pipelines.
   */
  protected String storeCollection;

  /**
   * If true then store provider should use local caching of the pipelines instead of hitting the database each time.
   * By default is disabled meaning it will read from the database the pipeline before each execution.
   */
  protected boolean storeCacheEnabled;

  /**
   * MongoPipe status collection, used for storing org.mongopipe.core.migration status. Inspired by Flyway SQL org.mongopipe.core.migration tool.
   */
  protected String migrationStatusCollection;

  protected PipelineStoreConfig() {
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
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

  public String getRepositoriesScanPackage() {
    return pipelineInterfacesScanPackage;
  }

  public void setPipelineInterfacesScanPackage(String pipelineInterfacesScanPackage) {
    this.pipelineInterfacesScanPackage = pipelineInterfacesScanPackage;
  }

  public String getStoreCollection() {
    return storeCollection;
  }

  public void setStoreCollection(String storeCollection) {
    this.storeCollection = storeCollection;
  }

  public boolean isStoreCacheEnabled() {
    return storeCacheEnabled;
  }

  public void setStoreCacheEnabled(boolean storeCacheEnabled) {
    this.storeCacheEnabled = storeCacheEnabled;
  }

  public String getMigrationStatusCollection() {
    return migrationStatusCollection;
  }

  public void setMigrationStatusCollection(String migrationStatusCollection) {
    this.migrationStatusCollection = migrationStatusCollection;
  }
}
