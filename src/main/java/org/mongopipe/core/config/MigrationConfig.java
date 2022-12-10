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

package org.mongopipe.core.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MigrationConfig {
  public static final String DEFAULT_PATH = "pipelines";
  @Builder.Default
  boolean enabled = true;
  @Builder.Default
  String pipelinesPath = DEFAULT_PATH;

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * If enabled migration will take place at startup.
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getPipelinesPath() {
    return pipelinesPath;
  }

  /**
   *  Sets the classpath path folder where pipeline files are located.
   */
  public void setPipelinesPath(String pipelinesPath) {
    this.pipelinesPath = pipelinesPath;
  }
}
