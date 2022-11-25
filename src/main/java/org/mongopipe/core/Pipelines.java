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

package org.mongopipe.core;

import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.migration.MigrationRunner;
import org.mongopipe.core.runner.PipelineRunner;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.store.PipelineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mongopipe.core.runner.context.RunContextProvider.DEFAULT_CONTEXT_ID;

/**
 * Factories for pipelines.
 */
public class Pipelines {
  private static final Logger LOG = LoggerFactory.getLogger(Pipelines.class);
  private static Map<String, PipelineStore> STORE_MAP = new HashMap<>();
  private static Map<String, PipelineRunner> RUNNER_MAP = new HashMap<>();

  /**
   * Create pipeline database store.
   * @param runConfigId in case you connect to multiple databases in the same process.
   * @returns the pipeline store for CRUD operations on pipelines.
   */
  public static PipelineStore getStore(String runConfigId) {
    RunContext runContext = RunContextProvider.getContext(runConfigId);
    if (runContext == null) {
      throw new MongoPipeConfigException("Mongo-pipe configuration is missing. Use 'Pipelines.newConfig()' for this.");
    }
    PipelineStore pipelineStore = new PipelineStore(runContext);
    STORE_MAP.put(runConfigId, pipelineStore);
    return pipelineStore;
  }

  /**
   * Create pipeline database store.
   * @returns the pipeline store for CRUD operations on pipelines.
   */
  public static PipelineStore getStore() {
    return getStore(DEFAULT_CONTEXT_ID);
  }

  /**
   * Create pipeline runner.
   * @param runConfigId in case you connect to multiple databases in the same process.
   * @returns runner for manually running pipeline without the need of @PipelineRun annotated interface methods.
   */
  public static PipelineRunner getRunner(String runConfigId) {
    PipelineRunner pipelineRunner = RUNNER_MAP.get(runConfigId);
    if (pipelineRunner != null) {
      return pipelineRunner;
    }
    RunContext runContext = RunContextProvider.getContext(runConfigId);
    if (runContext == null) {
      throw new MongoPipeConfigException("Create and register configuration first");
    }
    PipelineStore pipelineStore = STORE_MAP.get(runConfigId);
    if (pipelineStore == null) {
      pipelineStore = getStore(runConfigId);
    }
    pipelineRunner = new PipelineRunner(runContext, pipelineStore);
    RUNNER_MAP.put(runConfigId, pipelineRunner);
    return pipelineRunner;
  }

  /**
   * Create pipeline runner.
   * @returns runner for manually running pipeline without the need of @PipelineRun annotated interface methods.
   */
  public static PipelineRunner getRunner() {
    return getRunner(DEFAULT_CONTEXT_ID);
  }



  /**
   * Load pipelines for the configured pipeline source (default is to load them from classpath path) and update the ones that have changed.
   * This should be called at program startup.
   * @param runConfigId
   */
  public static void startMigration(String runConfigId) {
    new MigrationRunner(RunContextProvider.getContext(runConfigId), getStore(runConfigId)).run();
  }

  /**
   * @see Pipelines#startMigration(String)
   */
  public static void startMigration() {
    startMigration(DEFAULT_CONTEXT_ID);
  }
}
