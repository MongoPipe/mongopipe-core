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

public class CustomLogFactory {
  private static LogFactory logFactory;

  public static Log getLogger(String clazz) {
    if (logFactory == null) {
      if (classExists("org.slf4j.LoggerFactory")) {
        logFactory = new Slf4jLogFactory();
      } else if (classExists("org.apache.logging.log4j.Logger")) {
        logFactory = new Log4jLogFactory();
      } else {
        logFactory = new JavaUtilLogFactory();
      }
    }
    return logFactory.createLog(clazz);
  }

  public static Log getLogger(Class clazz) {
    return getLogger(clazz.getName());
  }

  private static boolean classExists(String clazz) {
    try {
      Class.forName(clazz);
    } catch (Exception e) {
      return false;
    }
    return true;
  }


}
