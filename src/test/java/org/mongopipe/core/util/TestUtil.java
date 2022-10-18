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

package org.mongopipe.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.BsonArray;
import org.bson.BsonDocumentReader;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.BsonArrayCodec;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtil {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


  public static List<Document> convertJsonArrayToDocumentList(String jsonArray) {

    DocumentCodec codec = new DocumentCodec();
    DecoderContext decoderContext = DecoderContext.builder().build();

    final CodecRegistry codecRegistry = CodecRegistries.fromProviders(Arrays.asList(new BsonValueCodecProvider()));
    JsonReader reader = new JsonReader(jsonArray);
    BsonArrayCodec arrayReader = new BsonArrayCodec(codecRegistry);
    BsonArray array = arrayReader.decode(reader, DecoderContext.builder().build());

    return array.stream()
        .map(BsonValue::asDocument)
        .map(bsonDocument ->
          codec.decode(new BsonDocumentReader(bsonDocument), decoderContext)
        )
        .collect(Collectors.toList());
  }

  public static String convertDocumentListToJson(List<Document> result) {
    // TODO: Consider using Document#toJSON instead.
    try {
      return OBJECT_MAPPER.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void assertJsonEqual(String json1, List<Document> result) {
    String json2 = convertDocumentListToJson(result);
    try {
      JSONAssert.assertEquals(json1, json2, false);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
}
