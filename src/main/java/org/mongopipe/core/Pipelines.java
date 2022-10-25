/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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
  public static final String DEFAULT_CONFIG_ID = "<configurationId>";
  private static final Logger LOG = LoggerFactory.getLogger(Pipelines.class);
  private static Map<String, PipelineRunConfig> CONFIG_MAP = new HashMap<>();
  private static Map<String, PipelineStore> STORE_MAP = new HashMap<>();
  private static Map<String, PipelineRunner> RUNNER_MAP = new HashMap<>();

  private static class PipelineRunConfigProxy extends PipelineRunConfig.Builder {
    public PipelineRunConfig build() {
      PipelineRunConfig pipelineRunConfig = super.build();
      if (pipelineRunConfig.getId() == null) {
        pipelineRunConfig.setId(DEFAULT_CONFIG_ID);
      }
      if (CONFIG_MAP.containsKey(pipelineRunConfig.getId())) {
        LOG.warn("Overwriting existing configuration with the same id.");
      }
      CONFIG_MAP.put(pipelineRunConfig.getId(), pipelineRunConfig);
      return pipelineRunConfig;
    }
  }

  public static PipelineRunConfig.Builder newConfig() {
    return new PipelineRunConfigProxy();
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
    PipelineStore pipelineStore = new PipelineStore(CONFIG_MAP.get(configurationId));
    STORE_MAP.put(configurationId, pipelineStore);
    return pipelineStore;
  }

  public static PipelineStore getStore() {
    return getStore(DEFAULT_CONFIG_ID);
  }

  public static PipelineRunner getRunner(String configurationId) {
    PipelineRunner pipelineRunner = RUNNER_MAP.get(configurationId);
    if (pipelineRunner != null) {
      return pipelineRunner;
    }
    PipelineRunConfig pipelineRunConfig = CONFIG_MAP.get(configurationId);
    if (pipelineRunConfig == null) {
      throw new MongoPipeConfigException("Create configuration first");
    }
    PipelineStore pipelineStore = STORE_MAP.get(configurationId);
    if (pipelineStore == null) {
      pipelineStore = getStore(configurationId);
    }
    pipelineRunner = new PipelineRunner(pipelineRunConfig, pipelineStore);
    RUNNER_MAP.put(configurationId, pipelineRunner);
    return pipelineRunner;
  }

  public static PipelineRunner getRunner() {
    return getRunner(DEFAULT_CONFIG_ID);
  }

  public static PipelineRunConfig getConfig(String configId) {
    return CONFIG_MAP.get(configId);
  }

}
