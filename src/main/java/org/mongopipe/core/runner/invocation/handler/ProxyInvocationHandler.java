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

import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.mongopipe.core.util.ReflectionUtil.getSimpleMethodId;

public class ProxyInvocationHandler implements InvocationHandler {
  private static final Log LOG = CustomLogFactory.getLogger(ProxyInvocationHandler.class);
  private final Class storeClass;
  private final Map<String, StoreMethodHandler> methodInvocationHandlers;

  public ProxyInvocationHandler(Class storeClass, Map<String, StoreMethodHandler> methodInvocationHandlers) {
    this.storeClass = storeClass;
    this.methodInvocationHandlers = methodInvocationHandlers;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      LOG.debug("Running store method {}#{}", method.getDeclaringClass().getCanonicalName(), method.getName());
      if (method.getDeclaringClass().equals(Object.class)) { // Diverge Object methods to avoid exceptions when being called.
        if (method.getName().equals("toString")) {
          return "proxy";
        } else {
          return null;
        }
      } else {
        String methodId = getSimpleMethodId(method);
        StoreMethodHandler storeMethodHandler = methodInvocationHandlers.get(methodId);
        return storeMethodHandler.run(proxy, method, args);
      }
    } catch (InvocationTargetException ex) {
      // throw the original exception to the caller.
      throw ex.getCause();
    }
  }

}
