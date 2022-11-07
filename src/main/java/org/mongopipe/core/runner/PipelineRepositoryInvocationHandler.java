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

package org.mongopipe.core.runner;

import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.PipelineRun;
import org.mongopipe.core.exception.MissingPipelineAnnotationException;
import org.mongopipe.core.store.PipelineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineRepositoryInvocationHandler implements InvocationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRepositoryInvocationHandler.class);
  private final PipelineStore pipelineStore;
  private final PipelineRunner pipelineRunner;
  private static List<Object> OBJECT_METHODS = Arrays.asList(Object.class.getDeclaredMethods()).stream().map(m -> m.getName()).collect(Collectors.toList());

  public PipelineRepositoryInvocationHandler(PipelineStore pipelineStore, PipelineRunner pipelineRunner) {
    this.pipelineStore = pipelineStore;
    this.pipelineRunner = pipelineRunner;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (!method.isAnnotationPresent(PipelineRun.class)) {
      if (OBJECT_METHODS.contains(method.getName())) {
        return String.class.getMethod(method.getName()).invoke(method.getName()); // TODO: remove, added for debug
      } else {
        throw new MissingPipelineAnnotationException("@Pipeline annotation missing on method:" + method.getName());
      }
    }
    String pipelineId = method.getAnnotation(PipelineRun.class).value();
    LOG.debug("Running pipeline {}", pipelineId);

    Map<String, Object> params = new HashMap<>();
    int argsIndex = 0;
    for (Parameter parameter : method.getParameters()) {
      params.put(parameter.getAnnotation(Param.class).value(), args[argsIndex++]);
    }

    return pipelineRunner.run(pipelineId, method, params);

  }
}
