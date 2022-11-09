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

import lombok.Builder;
import lombok.Data;

/**
 * Library main configuration.
 * NOTE: Create one config per Mongo database.
 */
@Data
@Builder
public class PipelineRunConfig {
  /**
   * Connection string URI for the Mongo database.
   * NOTE: For custom database connection provide your own MongoClient to
   * {@link org.mongopipe.core.config.PipelineRunContext#setMongoClient}
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
   * The Java package where to look for interfaces with @PipelineRun annotations.
   * This can be for example you application top package.
   */
  protected String scanPackage;

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
   * MongoPipe status collection, used for storing org.mongopipe.core.migration status.
   */
  protected String migrationStatusCollection;
}