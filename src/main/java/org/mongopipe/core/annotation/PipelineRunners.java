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

package org.mongopipe.core.annotation;

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.config.PipelineStoreConfig;

import java.lang.annotation.*;

/**
 * Optional annotation used to specify a specific configuration id in case the user has multiple databases or configurations.
 * All the pipelines from the annotated pipeline repository interface will use the {@link PipelineStoreConfig} identified by this
 * annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface PipelineRunners {

  /**
   * The {@link PipelineStoreConfig#id} in case user has multiple databases or multiple PipelineStoreConfig configurations.
   */
  String configurationId() default Pipelines.DEFAULT_CONFIG_ID;
}
