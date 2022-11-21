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
import lombok.Builder;
import lombok.Data;
import org.mongopipe.core.runner.context.RunContext;

/**
 * Library main configuration.
 * NOTE: Create one config per Mongo database.
 */
@Data
@Builder
public class MongoPipeConfig {
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
  protected String id;

  /**
   * Name of collection storing pipelines.
   */
  @Builder.Default
  protected String storeCollection = "pipeline_store";

  /**
   * If "true" when a pipeline is updated or deleted the old pipeline is moved into the pipeline_store_history collection.
   */
  protected boolean storeHistoryEnabled;

  @Builder.Default
  protected String storeHistoryCollection = "pipeline_store_history";

  @Builder.Default
  protected String statusCollection = "pipeline_status";
  /**
   * If true then store provider should use local caching of the pipelines instead of hitting the database each time.
   * By default is disabled meaning it will read from the database the pipeline before each execution.
   */
  protected boolean storeCacheEnabled;

}