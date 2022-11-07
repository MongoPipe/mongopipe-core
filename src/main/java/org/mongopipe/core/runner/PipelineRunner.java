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

package org.mongopipe.core.runner;


import org.bson.Document;
import org.mongopipe.core.runner.command.*;
import org.mongopipe.core.runner.command.param.AggregateParams;
import org.mongopipe.core.runner.command.param.FindOneAndUpdateParams;
import org.mongopipe.core.runner.command.param.UpdateManyParams;
import org.mongopipe.core.runner.command.param.UpdateOneParams;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.store.PipelineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Used as an alternative to pipeline repositories (interface classes and @PipelineRun annotations) to generically run pipelines.
 * Use the {@link org.mongopipe.core.Pipelines} class to create a runner. A runner is thread safe and does not need to be recreated.
 */
public class PipelineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);
  static Map<String, CommandSupplier> SUPPLIERS = new HashMap<>();

  // Consider also allowing the calling directly the driver API with the evaluated pipeline, in future.
  static {
    SUPPLIERS.put(AggregateParams.TYPE, (pipelineRun, pipelineRunConfig, parameters, returnPojoClass) ->
        new AggregateCommand(pipelineRun, pipelineRunConfig, parameters, returnPojoClass));
    SUPPLIERS.put(UpdateOneParams.TYPE, (pipelineRun, pipelineRunConfig, parameters, returnPojoClass) ->
        new UpdateOneCommand(pipelineRun, pipelineRunConfig, parameters, returnPojoClass));
    SUPPLIERS.put(UpdateManyParams.TYPE, (pipelineRun, pipelineRunConfig, parameters, returnPojoClass) ->
        new UpdateManyCommand(pipelineRun, pipelineRunConfig, parameters, returnPojoClass));
    SUPPLIERS.put(FindOneAndUpdateParams.TYPE, (pipelineRun, pipelineRunConfig, parameters, returnPojoClass) ->
        new FindOneAndUpdateCommand(pipelineRun, pipelineRunConfig, parameters, returnPojoClass));
  }

  private PipelineRunConfig pipelineRunConfig;
  private PipelineStore pipelineStore;

  public PipelineRunner(PipelineRunConfig pipelineRunConfig, PipelineStore pipelineStore) {
    this.pipelineRunConfig = pipelineRunConfig;
    this.pipelineStore = pipelineStore;
  }

  private <T> T run(Pipeline pipeline, Class returnPojoType, Class<T> returnContainerType, Map<String, ?> parameters) {
    validate(pipeline);
    Class pojoClass = returnPojoType != null ? returnPojoType : Document.class;
    if (pipeline.getResultClass() != null) {
      try {
        pojoClass = Class.forName(pipeline.getResultClass());
      } catch (ClassNotFoundException e) {
        LOG.error(e.getMessage(), e);
      }
    }
    CommandSupplier commandSupplier = SUPPLIERS.get(pipeline.getCommandAndParams() == null ?
        AggregateParams.TYPE : pipeline.getCommandAndParams().getType());
    Object result = commandSupplier.build(pipeline, pipelineRunConfig, parameters, pojoClass).run();

    return mapFinalResult(result, returnContainerType);
  }

  private void validate(Pipeline pipeline) {
    if (pipeline.getCollection() == null) {
      throw new MongoPipeConfigException("collectionName can not be null");
    }
  }

  private <T> T mapFinalResult(Object result, Class<T> returnContainerType) {
    if (Void.class.equals(returnContainerType.getClass())) {
      return null;
    }
    if (returnContainerType == null) {
      return (T) result;
    }
    if (List.class.equals(returnContainerType)) {
      if (result instanceof Iterable) {
        return (T) StreamSupport.stream(((Iterable) result).spliterator(), false)
            .collect(Collectors.toList());
      } else {
        throw new MongoPipeConfigException("Return type of pipeline interface method should be of type List<PojoClass> or List<Document>");
      }
    }
    if (Iterable.class.equals(returnContainerType)) {
      if (result instanceof Iterable) {
        return (T) result;
      } else {
        throw new MongoPipeConfigException("Return type of pipeline interface method should be of type Iterable");
      }
    } else if (Stream.class.equals(returnContainerType)) {
      if (result instanceof Iterable) {
        return (T) StreamSupport.stream(((Iterable) result).spliterator(), false);
      } else {
        return (T) Stream.of(result);
      }
    } else {
      return (T) result;
    }
  }

  public <T> Stream<T> run(String pipelineId, Class<T> returnClass, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, Stream.class, parameters);
  }

  public Stream<Document> run(String pipelineId, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), Document.class, Stream.class, parameters);
  }

  public <T> T run(String pipelineId, Map<String, ?> parameters, Class<T> returnClass) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, returnClass, parameters);
  }

  protected <T> T run(String pipelineId, Method pipelineRunMethod, Map<String, ?> parameters) {
    // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
    Class returnPojoClass = pipelineRunMethod.getReturnType();
    if (pipelineRunMethod.getGenericReturnType() instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType)pipelineRunMethod.getGenericReturnType();
      Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
      if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
        returnPojoClass = (Class) actualTypeArguments[0];
      }
    }
    return (T) run(pipelineStore.getPipeline(pipelineId), returnPojoClass, pipelineRunMethod.getReturnType(), parameters);
  }
}
