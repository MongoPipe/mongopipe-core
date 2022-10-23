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

package org.mongopipe.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps utility for Java 8.
 */
public class Maps {

  public static <K> Map<K, ?> of(K k1, Object v1, Object... others) {
    if (others.length % 2 != 0) {
      throw new RuntimeException("Need to specify an even number of elements in the order key,value,key,value, ...");
    }
    Map map = new HashMap();
    map.put(k1, v1);
    for (int i = 0; i< others.length; i++) {
      map.put(others[i], others[++i]);
    }
    return map;
  }

}
