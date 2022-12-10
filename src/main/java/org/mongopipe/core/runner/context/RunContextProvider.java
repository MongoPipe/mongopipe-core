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

import lombok.CustomLog;
import org.mongopipe.core.config.MongoPipeConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of the configurations.
 * With Spring the PipelineStore and PipelineRunner should probably be obtained on Pipelines#getStore by getting
 * the current active Spring profiles ids and searching for one of the ids, or actually not needed since Spring can provide bean based on
 * the current profiles active.
 */
@CustomLog
public class RunContextProvider {
  public static final String DEFAULT_CONTEXT_ID = "1"; // when process uses a single MongoDB database for running pipelines.
  private static final Map<String, RunContext> RUN_CONTEXT = Collections.synchronizedMap(new HashMap<>());

  public static RunContext getContext(String id) {
    // For Spring will probably will need to consider the active profile even for default id.
    // Will probably need to use Class.forName to integrate this with the Spring library transparently.
    return RUN_CONTEXT.get(id);
  }

  public static RunContext getContext() {
    return getContext(DEFAULT_CONTEXT_ID);
  }

  /**
   * Register the RunConfig.
   * @param mongoPipeConfig
   */
  public static RunContext registerConfig(MongoPipeConfig mongoPipeConfig) {
    if (mongoPipeConfig.getId() == null) {
      mongoPipeConfig.setId(DEFAULT_CONTEXT_ID);
    }
    if (RUN_CONTEXT.containsKey(mongoPipeConfig.getId())) {
      LOG.warn("Overwriting existing configuration with the same id.");
    }
    RunContext runContext = new RunContext(mongoPipeConfig);
    RUN_CONTEXT.put(mongoPipeConfig.getId(), runContext);
    return runContext;
  }
}
