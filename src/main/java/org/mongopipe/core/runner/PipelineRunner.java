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


import lombok.CustomLog;
import org.bson.Document;
import org.mongopipe.core.Stores;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.exception.MongoPipeRunException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.command.AggregateCommand;
import org.mongopipe.core.runner.command.CommandSupplier;
import org.mongopipe.core.runner.command.FindOneAndUpdateCommand;
import org.mongopipe.core.runner.command.UpdateManyCommand;
import org.mongopipe.core.runner.command.UpdateOneCommand;
import org.mongopipe.core.runner.command.param.AggregateParams;
import org.mongopipe.core.runner.command.param.FindOneAndUpdateOptions;
import org.mongopipe.core.runner.command.param.UpdateManyOptions;
import org.mongopipe.core.runner.command.param.UpdateOneOptions;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.store.PipelineStore;
import org.mongopipe.core.util.BsonUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.mongopipe.core.util.ReflectionUtil.getMethodGenericType;

/**
 * Used as an alternative to store classes (annotated with @Store) to generically run pipelines.
 * Use the {@link Stores} class to create a runner. A runner is thread safe and does not need to be recreated.
 */
@CustomLog
public class PipelineRunner {
  static Map<String, CommandSupplier> SUPPLIERS = new HashMap<>();

  // Consider also allowing the calling directly the driver API with the evaluated pipeline, in future.
  static {
    SUPPLIERS.put(AggregateParams.TYPE, (pipelineRun, context, parameters, returnPojoClass) ->
        new AggregateCommand(pipelineRun, context, parameters, returnPojoClass));
    SUPPLIERS.put(UpdateOneOptions.TYPE, (pipelineRun, context, parameters, returnPojoClass) ->
        new UpdateOneCommand(pipelineRun, context, parameters, returnPojoClass));
    SUPPLIERS.put(UpdateManyOptions.TYPE, (pipelineRun, context, parameters, returnPojoClass) ->
        new UpdateManyCommand(pipelineRun, context, parameters, returnPojoClass));
    SUPPLIERS.put(FindOneAndUpdateOptions.TYPE, (pipelineRun, context, parameters, returnPojoClass) ->
        new FindOneAndUpdateCommand(pipelineRun, context, parameters, returnPojoClass));
  }

  private RunContext runContext;
  private PipelineStore pipelineStore;

  public PipelineRunner(RunContext runContext, PipelineStore pipelineStore) {
    this.runContext = runContext;
    this.pipelineStore = pipelineStore;
  }

  /**
   * Allows running a pipeline event if it was not previously stored with PipelineStore in the db.
   * @param returnContainerClass  In case return type is a List, Iterable, Stream, etc
   */
  public <T> T run(Pipeline pipeline, Class returnClass, Class<T> returnContainerClass, Map<String, ?> parameters) {
    validate(pipeline);
    if (Arrays.asList(List.class, Stream.class, Iterable.class).contains(returnClass)) {
      returnContainerClass = returnClass;
      returnClass = Document.class;
    }
    Class pojoClass = returnClass != null ? returnClass : Document.class;
    CommandSupplier commandSupplier = SUPPLIERS.get(pipeline.getCommandOptions() == null ?
        AggregateParams.TYPE : pipeline.getCommandOptions().getType());
    Object result = commandSupplier.build(pipeline, runContext, parameters == null ? Collections.emptyMap() : parameters,
        pojoClass).run();

    return mapFinalResult(result, returnClass, returnContainerClass);
  }

  /**
   * @see PipelineRunner#run(Pipeline, Class, Class, Map)
   */
  public <T> T run(Pipeline pipeline, Class<T> returnClass, Map<String, ?> parameters) {
    return run(pipeline, returnClass, null, parameters);
  }
  public <T> T run(Pipeline pipeline, Class<T> returnClass) {
    return run(pipeline, returnClass, null, Collections.emptyMap());
  }

  private <T> T map(Object result, Class<T> resultClass) {
    if (result instanceof Document) {
      return BsonUtil.toPojo(((Document)result).toBsonDocument(), resultClass);
    } else {
      return (T) result;
    }
  }

  private <T> T mapFinalResult(Object result, Class resultClass, Class<T> returnContainerClass) {
    if (result == null) {
      return (T) result;
    }
    if (Void.class.equals(returnContainerClass)) {
      return null;
    }

    if (returnContainerClass == null) {
      return (T)map(result, resultClass);
    } else {
      if (List.class.equals(returnContainerClass)) {
        if (result instanceof Iterable) {
          return (T) StreamSupport.stream(((Iterable) result).spliterator(), false)
              .collect(Collectors.toList());
        } else {
          return (T) map(result, resultClass);
        }
      }

      if (Iterable.class.equals(returnContainerClass)) {
        if (result instanceof Iterable) {
          return (T) result;
        } else {
          throw new MongoPipeConfigException("Return type of pipeline interface method should be of type Iterable");
        }
      }

      if (Stream.class.equals(returnContainerClass)) {
        if (result instanceof Iterable) {
          return (T) StreamSupport.stream(((Iterable) result).spliterator(), false);
        } else {
          return (T) Stream.of(result);
        }
      }
      return (T) result;
    }
  }

  /**
   * Runs a pipeline by id.
   * @param containerClass  A container class e.g. Stream.class, List.class, Iterable.class.
   */
  public <T> T run(String pipelineId, Class returnClass, Class<T> containerClass, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, containerClass, parameters);
  }

  public <T> T run(String pipelineId, Class<T> returnClass, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, null, parameters);
  }

  public <T> T run(String pipelineId, Class<T> returnClass) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, null, Collections.emptyMap());
  }

  public Stream<Document> runAndStream(String pipelineId, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), Document.class, Stream.class, parameters);
  }

  /**
   * Runs the pipeline and returns the results as a stream of elements of the given type.
   * E.g.: <code>
   *   Pipelines.getRunner().runAndList("matchingPizzasBySize", Pizza.class, Maps.of("pizzaSize", "medium"))
   * </code>
   */
  public <T> Stream<T> runAndStream(String pipelineId, Class<T> elementClass, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), elementClass, Stream.class, parameters);
  }


  public List<Document> runAndList(String pipelineId, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), null, List.class, parameters);
  }

  /**
   * Runs the pipeline and returns the results as a list of elements of given type.
   * E.g.: <code>
   *   Pipelines.getRunner().runAndList("matchingPizzasBySize", Pizza.class, Maps.of("pizzaSize", "medium"))
   * </code>
   */
  public <T> List<T> runAndList(String pipelineId, Class<T> elementClass, Map<String, ?> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), elementClass, List.class, parameters);
  }


  public <T> T run(String pipelineId, Map<String, ?> parameters, Class<T> returnClass) {
    return (T) run(pipelineStore.getPipeline(pipelineId), returnClass, getContainerReturnType(returnClass), parameters);
  }

  private Class getContainerReturnType(Class containerType) {
    // Currently can map only to this containers.
    return Arrays.asList(List.class, Stream.class, Iterable.class).contains(containerType) ? containerType : null;
  }

  protected <T> T run(String pipelineId, Method pipelineRunMethod, Map<String, ?> parameters) {
    Pipeline pipeline = pipelineStore.getPipeline(pipelineId);
    if (pipeline == null) {
      throw new MongoPipeRunException("Pipeline not found in store(database) for id: " + pipelineId);
    }
    return (T) run(pipeline, getMethodGenericType(pipelineRunMethod), getContainerReturnType(pipelineRunMethod.getReturnType()),
        parameters);
  }

  private void validate(Pipeline pipeline) {
    // Allow running pipelines without an id, to make the api easier to use, and for pipelines not previously saved in db.
    if (pipeline.getCollection() == null) {
      throw new MongoPipeConfigException("Collection name can not be null");
    }
  }
}
