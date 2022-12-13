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

package org.mongopipe.core.logging;

import org.slf4j.Logger;

public class Slf4jLog implements Log {
  private final Logger logger;

  public Slf4jLog(Logger logger) {
    this.logger = logger;
  }

  public void debug(String message, Object... params) {
    logger.debug(message, params);
  }

  public void info(String message, Object... params) {
    logger.info(message, params);
  }

  public void warn(String message, Object... params) {
    logger.warn(message, params);
  }

  public void error(String message, Object... params) {
    logger.error(message, params);
  }

  @Override
  public void error(String message, Exception exception) {
    logger.error(message, exception);
  }

}
