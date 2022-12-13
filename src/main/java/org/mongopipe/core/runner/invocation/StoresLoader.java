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

package org.mongopipe.core.runner.invocation;

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.Store;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.runner.invocation.handler.CrudInvocationHandler;
import org.mongopipe.core.runner.invocation.handler.DefaultMethodInvocationHandler;
import org.mongopipe.core.runner.invocation.handler.PipelineInvocationHandler;
import org.mongopipe.core.runner.invocation.handler.ProxyInvocationHandler;
import org.mongopipe.core.runner.invocation.handler.StoreMethodHandler;
import org.mongopipe.core.store.CrudStore;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mongopipe.core.util.ReflectionUtil.getClassMethodsIncludingInherited;
import static org.mongopipe.core.util.ReflectionUtil.getSimpleMethodId;

public class StoresLoader {
  private static final Log LOG = CustomLogFactory.getLogger(StoresLoader.class);
  private final Map<Class, Object> stores = Collections.synchronizedMap(new HashMap());
  DefaultMethodInvocationHandler defaultedMethodInvocationHandler = new DefaultMethodInvocationHandler();

  private <T> T loadStore(Class<T> storeClass) {
    if (!storeClass.isAnnotationPresent(Store.class)) {
      throw new MongoPipeConfigException("Missing annotation on the store.");
    }
    String configId = storeClass.getAnnotation(Store.class).configurationId();
    // Get the configuration corresponding to the store.
    RunContext runContext = RunContextProvider.getContext(configId);
    if (runContext == null) {
      throw new MongoPipeConfigException("Missing configuration with id " + configId);
    }
    validateMethods(storeClass);
    Map<String, StoreMethodHandler> methodInvocationHandlers = createInvocationHandlers(storeClass, runContext);

    ProxyInvocationHandler invocationHandler = new ProxyInvocationHandler(storeClass, methodInvocationHandlers);
    // https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
    T store = (T) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class[]{storeClass},
        invocationHandler);
    stores.put(storeClass, store);
    LOG.debug("Created store {}", storeClass.getCanonicalName());

    return store;
  }

  private StoreMethodHandler createMethodHandler(Method method, Class storeClass, RunContext runContext) {
    Optional<Method> crudMethodOptional = Arrays.asList(CrudStore.class.getDeclaredMethods()).stream()
        .filter(crudMethod -> crudMethod.getName().equals(method.getName()))
        .filter(crudMethod -> crudMethod.getParameterCount() == method.getParameterCount())
        // .filter TODO: add parameter matching also
        .findFirst();
    if (crudMethodOptional.isPresent()) {
      // This is a CRUD method matching the ones in CrudStore then delegate accordingly.
      return new CrudInvocationHandler(crudMethodOptional.get(), storeClass, runContext);
    } else if (method.isDefault()) {
      return defaultedMethodInvocationHandler;
    } else {
      // Fallback on pipeline, try to find a pipeline with the id "className#methodName" in case @PipelineRun is not provided.
      return new PipelineInvocationHandler(method, storeClass, runContext);
    }
    
  }

  private <T> Map<String, StoreMethodHandler> createInvocationHandlers(Class<T> storeClass, RunContext runContext) {
    Map<String, StoreMethodHandler> handlers = new HashMap<>();
    // Note that method may be from a super interface and not be declared in storeClass, so need to pass both parameters.
    getClassMethodsIncludingInherited(storeClass).stream()
        .forEach(method -> handlers.put(getSimpleMethodId(method), createMethodHandler(method, storeClass, runContext)));
    return handlers;
  }

  private void validateMethods(Class storeClass) {
    Arrays.stream(storeClass.getDeclaredMethods()).forEach(method -> {
      for (Parameter parameter : method.getParameters()) {
        // NOTE: By default do not enforce annotations, and use the parameter order to replace the corresponding variable.
        if (parameter.isAnnotationPresent(Param.class)) {
          String paramName = parameter.getAnnotation(Param.class).value();
          if (!paramName.matches("\\S+")) {
            throw new MongoPipeConfigException("@Param name should not contain whitespaces: '" + paramName + "' on "
                + method.getDeclaringClass() + "#" + method.getName());
          }
        }
      }
    });
  }

  /**
   * @returns a store instantiating the given storeClass interface.
   */
  public <T> T getStore(Class<T> storeClass) {
    // Lazy load the store on first usage.
    // TODO: In the Spring integration library a Class.forName("...").newInstance() will allow calling an adapter from the library for
    //       registering all stores as Spring beans as they are created.
    //       Also will need to handle Spring's org.springframework.context.annotation.Profile annotation to activate or not beans.
    return (T) Optional.ofNullable(stores.get(storeClass)).orElse(loadStore(storeClass));
  }
}
