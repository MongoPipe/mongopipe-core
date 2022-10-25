/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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


import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.bson.BsonDocument;
import org.bson.Document;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.exception.InvalidPipelineTemplateException;
import org.mongopipe.core.model.PipelineCommandType;
import org.mongopipe.core.model.PipelineRun;
import org.mongopipe.core.store.PipelineStore;
import org.mongopipe.core.util.BsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.mongopipe.core.util.BsonUtil.toBsonDocumentList;

public class PipelineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);

  private PipelineRunConfig pipelineRunConfig;
  private PipelineStore pipelineStore;

  public PipelineRunner(PipelineRunConfig pipelineRunConfig, PipelineStore pipelineStore) {
    this.pipelineRunConfig = pipelineRunConfig;
    this.pipelineStore = pipelineStore;
  }

  private <T> T run(PipelineRun pipelineRun, Class returnPojoType, Class<T> returnContainerType, Map<String, Serializable> parameters) {

    // TODO: Refactor.
    if (pipelineRun.getCommandType() == null || pipelineRun.getCommandType() == PipelineCommandType.AGGREGATE) {

      String rawPipeline = BsonUtil.toString(pipelineRun.getPipeline()); // Convert to json to evaluate with freemarker, and then back.

      // 1. Configure FreeMarker
      //
      // You should do this ONLY ONCE, when your application starts,
      // then reuse the same Configuration object elsewhere.
      Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);

      cfg.setDefaultEncoding("UTF-16");
      try {
        Template template = new Template("name", new StringReader(rawPipeline), cfg);
        StringWriter out = new StringWriter();
        template.process(parameters, out);
        //System.out.println(out.getBuffer().toString());
        out.flush();
        rawPipeline = out.toString();
      } catch (IOException | TemplateException e) {
        throw new InvalidPipelineTemplateException("Invalid template", e);
      }

      List<BsonDocument> bsonList = toBsonDocumentList(rawPipeline); // Now convert it back to bson.
      Class pojoClass = returnPojoType != null ? returnPojoType : Document.class;
      if (pipelineRun.getResultClass() != null) {
        try {
          pojoClass = Class.forName(pipelineRun.getResultClass());
        } catch (ClassNotFoundException e) {
          LOG.error(e.getMessage(), e);
        }
      }

      MongoCollection mongoCollection = pipelineRunConfig.getMongoDatabase().getCollection(pipelineRun.getCollection());
      AggregateIterable iterable = mongoCollection.aggregate(bsonList, pojoClass);

      if (Collection.class.isAssignableFrom(returnContainerType)) {
        return (T) StreamSupport.stream(iterable.spliterator(), false)
            .collect(Collectors.toList());
      } else if (Iterable.class.isAssignableFrom(returnContainerType)) {
        return (T) iterable;
      } else if (Stream.class.isAssignableFrom(returnContainerType)) {
        return (T) StreamSupport.stream(iterable.spliterator(), false);
      } else {
        throw new RuntimeException("not implemented");
      }
    } else {
      // TODO: implement the other operations also, consider command pattern or strategy.
      return null;
    }
  }

  public <T> Stream<T> run(String pipelineId, Class<T> returnClass, Map<String, Serializable> parameters) {
    return run(pipelineStore.getPipeline(pipelineId), returnClass, Stream.class, parameters);
  }


  protected <T> T run(String pipelineId, Method pipelineRunMethod, Map<String, Serializable> parameters) {
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
