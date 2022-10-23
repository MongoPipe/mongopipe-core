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

package org.mongopipe.core.util;

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class JsonUtil {
  public static void assertJsonEqual(String json1, List<Document> result) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < result.size() - 1; i++) {
      sb.append(result.get(i).toJson());
      sb.append(",");
    }
    if (result.size() >= 1) {
      sb.append(result.get(result.size() - 1).toJson());
    }
    sb.append("]");

    try {
      JSONAssert.assertEquals(json1, sb.toString(), false);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: This is only for tests, might be replaced with gson or jackson, so this type information like $date will not be present anymore.
  public static <T> String convertPojoToJson(List<T> result) {
    StringBuilder sb = new StringBuilder("[");

    CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder()
            .automatic(true)
            .build()));

    Encoder encoder = pojoCodecRegistry.get(result.get(0).getClass()); // improve

      for (int i = 0; i < result.size() - 1; i++) {
        BsonDocument unwrapped = new BsonDocument();
        BsonWriter writer = new BsonDocumentWriter(unwrapped);
        encoder.encode(writer, result.get(i), EncoderContext.builder().build());        ;
        sb.append(unwrapped.toJson());
        sb.append(",");
      }
      if (result.size() >= 1) {
        BsonDocument unwrapped = new BsonDocument();
        BsonWriter writer = new BsonDocumentWriter(unwrapped);
        encoder.encode(writer, result.get(result.size() - 1), EncoderContext.builder().build());        ;
        sb.append(unwrapped.toJson());
      }
      sb.append("]");

    return sb.toString();

  }
}
