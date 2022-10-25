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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.bson.Document;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestUtil {
  public static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  static {
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public static void assertJsonEqual(String json1, List<Document> result) {
    //    StringBuilder sb = new StringBuilder("[");
    //    for (int i = 0; i < result.size() - 1; i++) {
    //      sb.append(result.get(i).toJson());
    //      sb.append(",");
    //    }
    //    if (result.size() >= 1) {
    //      sb.append(result.get(result.size() - 1).toJson());
    //    }
    //    sb.append("]");
    //
    try {
      JSONAssert.assertEquals(json1, OBJECT_MAPPER.writeValueAsString(result), false);
    } catch (JSONException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }




  public static <T> String convertPojoToJson(T pojo) {
    // Using BSON library. Keep for reference, might need improvement. Adds extra type information like $date for dates.
    //    StringBuilder sb = new StringBuilder("[");
    //
    //    CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
    //        CodecRegistries.fromProviders(PojoCodecProvider.builder()
    //            .automatic(true)
    //            .build()));
    //
    //    Encoder encoder = pojoCodecRegistry.get(result.get(0).getClass()); // improve
    //
    //    for (int i = 0; i < result.size() - 1; i++) {  // List<T> result
    //      BsonDocument unwrapped = new BsonDocument();
    //      BsonWriter writer = new BsonDocumentWriter(unwrapped);
    //      encoder.encode(writer, result.get(i), EncoderContext.builder().build());        ;
    //      sb.append(unwrapped.toJson());
    //      sb.append(",");
    //    }
    //    if (result.size() >= 1) {
    //      BsonDocument unwrapped = new BsonDocument();
    //      BsonWriter writer = new BsonDocumentWriter(unwrapped);
    //      encoder.encode(writer, result.get(result.size() - 1), EncoderContext.builder().build());        ;
    //      sb.append(unwrapped.toJson());
    //    }
    //    sb.append("]");
    // return sb.toString();

    try {
      return OBJECT_MAPPER.writeValueAsString(pojo);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getClasspathFileContent(String resourcePath) {
    try {
      return new String(Files.readAllBytes(Paths.get(BsonUtil.class.getClassLoader().getResource(resourcePath).toURI())));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
