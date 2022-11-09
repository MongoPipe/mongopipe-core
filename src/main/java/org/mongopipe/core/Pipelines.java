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

import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.config.PipelineRunContext;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.runner.PipelineRepositoriesLoader;
import org.mongopipe.core.runner.PipelineRunner;
import org.mongopipe.core.store.PipelineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factories for most of the API.
 */
public class Pipelines {
  public static final String DEFAULT_CONTEXT_ID = "<configurationId>"; // If you want to use concurrently multiple MongoDB databases.
  private static final Logger LOG = LoggerFactory.getLogger(Pipelines.class);
  private static Map<String, PipelineRunContext> RUN_CONTEXT = new HashMap<>();
  private static Map<String, PipelineStore> STORE_MAP = new HashMap<>();
  private static Map<String, PipelineRunner> RUNNER_MAP = new HashMap<>();

  public static void registerConfig(PipelineRunConfig pipelineRunConfig) {
      if (pipelineRunConfig.getId() == null) {
        pipelineRunConfig.setId(DEFAULT_CONTEXT_ID);
      }
      if (RUN_CONTEXT.containsKey(pipelineRunConfig.getId())) {
        LOG.warn("Overwriting existing configuration with the same id.");
      }
      PipelineRunContext pipelineRunContext = new PipelineRunContext(pipelineRunConfig, null);
      RUN_CONTEXT.put(pipelineRunConfig.getId(), pipelineRunContext);
  }

  /**
   * Obtains a pipeline interface implementation that you can use to call your pipelines.
   * A. Without Spring (mongopipe-core):
   * Pipelines.from(MyRestaurant.class)
   *     .getPizzaOrdersBySize("MEDIUM");
   *
   * B. With Spring (mongopipe-spring):
   * @Autowired
   * MyRestaurant myRestaurant; // No need to call 'Pipelines.from'.
   * ...
   * myRestaurant.getPizzaOrdersBySize("MEDIUM", ...);
   * ```
   * @param pipelineRepositoryInterface is your interface class where you marked pipelines using the @Pipeline annotation.
   * @param <T> The actual implementation proxy.
   * @return
   */
  public static <T> T from(Class<T> pipelineRepositoryInterface) {
    return PipelineRepositoriesLoader.getRepository(pipelineRepositoryInterface);
  }

  /**
   * Returns a pipeline store allowing CRUD operations on the pipeline.
   * @param configurationId
   * @return
   */
  public static PipelineStore getStore(String configurationId) {
    PipelineRunContext pipelineRunContext = RUN_CONTEXT.get(configurationId);
    if (pipelineRunContext == null) {
      throw new MongoPipeConfigException("Mongo-pipe configuration is missing. Use 'Pipelines.newConfig()' for this.");
    }
    PipelineStore pipelineStore = new PipelineStore(pipelineRunContext);
    STORE_MAP.put(configurationId, pipelineStore);
    return pipelineStore;
  }

  /**
   * @returns the pipeline store that can be used to do CRUD operations on pipelines.
   */
  public static PipelineStore getStore() {
    return getStore(DEFAULT_CONTEXT_ID);
  }

  public static PipelineRunner getRunner(String configurationId) {
    PipelineRunner pipelineRunner = RUNNER_MAP.get(configurationId);
    if (pipelineRunner != null) {
      return pipelineRunner;
    }
    PipelineRunContext pipelineRunContext = RUN_CONTEXT.get(configurationId);
    if (pipelineRunContext == null) {
      throw new MongoPipeConfigException("Create and register configuration first");
    }
    PipelineStore pipelineStore = STORE_MAP.get(configurationId);
    if (pipelineStore == null) {
      pipelineStore = getStore(configurationId);
    }
    pipelineRunner = new PipelineRunner(pipelineRunContext, pipelineStore);
    RUNNER_MAP.put(configurationId, pipelineRunner);
    return pipelineRunner;
  }

  public static PipelineRunner getRunner() {
    return getRunner(DEFAULT_CONTEXT_ID);
  }

  public static PipelineRunContext getRunContext(String configId) {
    return RUN_CONTEXT.get(configId);
  }
  public static PipelineRunContext getRunContext() {
    return RUN_CONTEXT.get(DEFAULT_CONTEXT_ID);
  }

}
