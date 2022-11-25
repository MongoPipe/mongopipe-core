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

package org.mongopipe.core.runner.invocation.handler;

import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.store.MongoCrudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Calls methods on MongoCrudStore for methods that match to a MongoCrudStore method.
 */
public class CrudInvocationHandler implements InvocationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(CrudInvocationHandler.class);
  private final Method mongoCrudMethod;
  private final MongoCrudStore mongoCrudStore;

  public CrudInvocationHandler(Method matchingCrudMethod, Class storeClass, RunContext runContext) {
    this.mongoCrudMethod = matchingCrudMethod;
    this.mongoCrudStore = new MongoCrudStore(runContext, storeClass);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object result = mongoCrudMethod.invoke(mongoCrudStore, args);

    // Result default mappings
    if (method.getReturnType() != mongoCrudMethod.getReturnType()) {
      if (result instanceof Optional) {
        return ((Optional)result).get();
      }
    }
    return result;
  }
}
