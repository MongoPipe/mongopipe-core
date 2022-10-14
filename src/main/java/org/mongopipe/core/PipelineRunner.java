/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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

package org.mongopipe.core;


import com.mongodb.client.AggregateIterable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.codecs.BsonArrayCodec;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.exception.InvalidPipelineTemplateException;
import org.mongopipe.core.model.PipelineOperationType;
import org.mongopipe.core.model.PipelineRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PipelineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);

  private PipelineRunConfig pipelineRunConfig;
  private PipelineStore pipelineStore;

  public PipelineRunner(PipelineRunConfig pipelineRunConfig, PipelineStore pipelineStore) {
    this.pipelineRunConfig = pipelineRunConfig;
    this.pipelineStore = pipelineStore;
  }

  public <T> T run(PipelineRun pipelineRun, Class<T> returnClass, Map<String, Serializable> parameters) {

    // TODO: Refactor.
    if (pipelineRun.getOperationType() == null || pipelineRun.getOperationType() == PipelineOperationType.AGGREGATE) {

      String rawPipeline = pipelineRun.getPipeline(); // TODO: evaluate template.


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
      //Document.parse(rawPipeline);
      // https://splunktool.com/jsonparse-equivalent-in-mongo-driver-3x-for-java
      // https://stackoverflow.com/questions/34436952/json-parse-equivalent-in-mongo-driver-3-x-for-java
      final CodecRegistry codecRegistry = CodecRegistries.fromProviders(Arrays.asList(new BsonValueCodecProvider()));
      JsonReader reader = new JsonReader(rawPipeline);
      BsonArrayCodec arrayReader = new BsonArrayCodec(codecRegistry);
      BsonArray array = arrayReader.decode(reader, DecoderContext.builder().build());
      List<BsonDocument> bsonList = array.stream()
          .map(BsonValue::asDocument)
          .collect(Collectors.toList());

      AggregateIterable iterable = pipelineRunConfig.getMongoDatabase().getCollection(pipelineRun.getCollection()).aggregate(bsonList);

      if (Collection.class.isAssignableFrom(returnClass)) {
        return (T) StreamSupport.stream(iterable.spliterator(), false)
            .collect(Collectors.toList());
      } else if (Iterable.class.isAssignableFrom(returnClass)) {
        return (T) iterable;
      } else if (Stream.class.isAssignableFrom(returnClass)) {
        return (T) StreamSupport.stream(iterable.spliterator(), false);
      } else {
        throw new RuntimeException("not implemented"); // TODO. Also check that POJO type conversion happens correctly.
      }
    } else {
      // TODO: implement the other operations also, consider command pattern or strategy.
      return null;
    }
  }

  public <T> T run(String pipelineId, Class<T> returnClass, Map<String, Serializable> parameters) {
    return (T) run(pipelineStore.getPipeline(pipelineId), returnClass, parameters);
  }

}
