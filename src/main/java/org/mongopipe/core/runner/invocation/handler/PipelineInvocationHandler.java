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

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.PipelineRun;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.exception.PipelineNotFoundException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.context.RunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static org.mongopipe.core.util.ReflectionsUtil.getMethodGenericType;

/**
 * Calls a database stored pipeline. Called on any methods that:
 * - are annotated with @PipelineRun
 * - are not annotated with @PipelineRun and they do not match any possible CRUD method but a pipeline exists with the pipeline id being
 *   "storeClassName.methodName".
 */
public class PipelineInvocationHandler implements InvocationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineInvocationHandler.class);
  private final Class storeClass;
  private final RunContext runContext;
  private final Method method;

  public PipelineInvocationHandler(Method method, Class storeClass, RunContext runContext) {
    this.storeClass = storeClass;
    this.runContext = runContext;
    this.method = method;
  }

  @Override
  public Object invoke(Object proxy, Method runMethod, Object[] args) {
    String pipelineId;
    boolean isAnnotationPresent = method.isAnnotationPresent(PipelineRun.class);
    pipelineId = isAnnotationPresent ? method.getAnnotation(PipelineRun.class).value() :
        storeClass.getSimpleName() + "." + method.getName();

    Pipeline pipeline = Pipelines.getStore(runContext.getId()).getPipeline(pipelineId);
    if (pipeline == null) {
      if (isAnnotationPresent) {
        throw new PipelineNotFoundException(pipelineId);
      } else {
        throw new MongoPipeConfigException("Method '" + method.toString() + "' naming does not matches any CRUD convention method naming " +
            "nor any existing pipeline with an id equal to 'className.methodName'.");
      }
    }

    Map<String, Object> params = new HashMap<>();
    int argsIndex = 0, totalAnnotatedParams = 0;
    for (Parameter parameter : method.getParameters()) {
      String paramName = String.valueOf(argsIndex + 1);  // If @Param is missing use the parameter index as a name.
      if (parameter.isAnnotationPresent(Param.class)) {
        totalAnnotatedParams++;
        paramName = parameter.getAnnotation(Param.class).value();
      }
      params.put(paramName, args[argsIndex++]);
    }
    if (totalAnnotatedParams > 0 && totalAnnotatedParams < method.getParameters().length) {
      throw new MongoPipeConfigException("All method parameters should use @Param annotation or no parameter at all");
    }

    Class returnPojoClass = getMethodGenericType(method);

    return Pipelines.getRunner(runContext.getId()).run(pipeline, returnPojoClass, method.getReturnType(), params);
  }
}
