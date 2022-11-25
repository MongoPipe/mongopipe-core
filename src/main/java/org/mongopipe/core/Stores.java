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

import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.runner.invocation.StoresLoader;
import org.mongopipe.core.store.PipelineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores factory. The user provides the interface containing both @PipelineRun methods and CRUD methods and receives an implementation for
 * his interface allowing execution of DB stored pipelines or CRUD operations.
 */
public class Stores {
  private static final Logger LOG = LoggerFactory.getLogger(Stores.class);
  private static StoresLoader storesLoader = new StoresLoader();

  /**
   * Obtains a store implementation that you can use to call your pipelines or other crud operations.
   * A. Without Spring (mongopipe-core):
   * Stores.from(MyRestaurant.class)
   *     .getPizzaOrdersBySize("MEDIUM");
   *
   * B. With Spring (mongopipe-spring):
   * @Autowired
   * MyRestaurant myRestaurant; // No need to call 'Pipelines.from'.
   * ...
   * myRestaurant.getPizzaOrdersBySize("MEDIUM", ...);
   * ```
   *
   * NOTE: If @PipelineRun annotation is not used then specific methods will map to specific CRUD actions based on the naming conventions.
   *       If the method name can not be mapped to viable CRUD action then the method name will be matched to a database pipeline with the
   *       id equal to the "<store simple class name>.<method name>". If this also fails then an exception is thrown at startup time.
   * @param storeClass is your interface class marked with @Store.
   * @param <T> The actual implementation proxy.
   * @return
   */
  public static <T> T get(Class<T> storeClass) {
    if (PipelineStore.class.equals(storeClass)) { // TODO: unify branches
      return (T)Pipelines.getStore();
    } else {
      return storesLoader.getStore(storeClass); // @Store annotation contains the config id.
    }
  }

  /**
   * Returns a pipeline database store by default.
   * @returns a pipeline database store for doing CRUD operations on pipelines.
   */
  public static PipelineStore getPipelineStore() {
    return Pipelines.getStore();
  }

  /**
   * Register the base configuration.
   * @param mongoPipeConfig
   */
  public static void registerConfig(MongoPipeConfig mongoPipeConfig) {
    RunContextProvider.registerConfig(mongoPipeConfig);
  }

}
