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

package org.mongopipe.core.util;

import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonReader;
import org.bson.types.ObjectId;
import org.mongopipe.core.exception.MongoPipeConfigException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mongopipe.core.config.PojoCodecConfig.getCodecRegistry;

public class BsonUtil {

  public static String toString(List<BsonDocument> bsonDocumentList) {
    return bsonDocumentList.toString();
  }

  /**
   * @returns a BsonDocument list from the bson string.
   */
  public static List<BsonDocument> toBsonList(String bson) {
    // Document.parse(rawPipeline);
    // http://mongodb.github.io/mongo-java-driver/3.7/driver/getting-started/quick-start-pojo/
    // https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/document-data-format-pojo/
    // https://splunktool.com/jsonparse-equivalent-in-mongo-driver-3x-for-java
    // https://stackoverflow.com/questions/34436952/json-parse-equivalent-in-mongo-driver-3-x-for-java
    final CodecRegistry codecRegistry = CodecRegistries.fromProviders(Collections.singletonList(new BsonValueCodecProvider()));
    // JsonReader reader = new JsonReader(bson);
    JsonReader reader = new JsonReader(bson);
    BsonArrayCodec arrayReader = new BsonArrayCodec(codecRegistry);
    BsonArray array = arrayReader.decode(reader, DecoderContext.builder().build());
    return array.stream()
        .map(BsonValue::asDocument)
        .collect(Collectors.toList());
  }

  public static Document toDocument(String bson) {
    // same Document.parse(bson);
    JsonReader reader = new JsonReader(bson);
    Codec<Document> decoder = getCodecRegistry().get(Document.class);
    return decoder.decode(reader, DecoderContext.builder().build());
  }

  public static Document toDocument(Bson bson) {
    DocumentCodec codec = new DocumentCodec();
    DecoderContext decoderContext = DecoderContext.builder().build();
    return codec.decode(new BsonDocumentReader(bson.toBsonDocument()), decoderContext);
  }

  public static <T> T toPojo(BsonDocument bsonDocument, Class<T> pojoClass) {
    BsonReader reader = new BsonDocumentReader(bsonDocument);

    //CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
    //    CodecRegistries.fromProviders(PojoCodecProvider.builder()
    //        .automatic(true)
    //        .build()));
    CodecRegistry pojoCodecRegistry = getCodecRegistry();

    Decoder<T> encoder = pojoCodecRegistry.get(pojoClass);
    T pojo = encoder.decode(reader, DecoderContext.builder().build());
    return pojo;
  }

  private static Document toDocument(BsonDocument bsonDocument) {
    DocumentCodec codec = new DocumentCodec();
    DecoderContext decoderContext = DecoderContext.builder().build();
    return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
  }

  /**
   * Converts a String from a classpath resource to a List of Documents.
   */
  public static List<Document> loadResourceIntoDocumentList(String resourcePath) {
    try {
      String bson = new String(Files.readAllBytes(Paths.get(BsonUtil.class.getClassLoader().getResource(resourcePath).toURI())));
      return toBsonList(bson).stream()
          .map(elem -> (BsonDocument) elem)
          .map(BsonUtil::toDocument)
          .collect(Collectors.toList());
    } catch (IOException | URISyntaxException e) {
      throw new MongoPipeConfigException("Can not load classpath file " + resourcePath, e);
    }
  }

  public static List<BsonDocument> loadResourceIntoBsonDocumentList(String resourcePath) {
    try {
      String bson = new String(Files.readAllBytes(Paths.get(BsonUtil.class.getClassLoader().getResource(resourcePath).toURI())));
      return toBsonList(bson);
    } catch (IOException | URISyntaxException e) {
      throw new MongoPipeConfigException("Can not load classpath file " + resourcePath, e);
    }
  }


  public static <T> T toPojo(String bsonString, Class<T> pojoClass) {
    try {
      // see bson2pojo https://stackoverflow.com/questions/71777864/how-to-convert-a-pojo-to-an-bson-using-mongodb-java-driver
      CodecRegistry pojoCodecRegistry = getCodecRegistry();
      // Document.parse(bsonString).toBsonDocument();
      BsonDocument bsonDocument = new BsonDocumentCodec().decode(new JsonReader(bsonString), DecoderContext.builder().build());

      BsonReader reader = new BsonDocumentReader(bsonDocument);
      Decoder<T> decoder = pojoCodecRegistry.get(pojoClass);
      T pojo = decoder.decode(reader, DecoderContext.builder().build());
      return pojo;

    } catch (RuntimeException e) {
      throw new MongoPipeConfigException("Can not convert bson string to pojo:" + bsonString);
    }
  }

  /**
   * Converts a String from a classpath resource to pojo class.
   */
  public static <T> T loadResourceIntoPojo(String resourcePath, Class<T> pojoClass) {
    try {
      String bsonString = new String(Files.readAllBytes(Paths.get(BsonUtil.class.getClassLoader().getResource(resourcePath).toURI())));
      return toPojo(bsonString, pojoClass);
    } catch (URISyntaxException | IOException e) {
      throw new MongoPipeConfigException("Can not convert resource path to pojo:" + resourcePath, e);
    }
  }

  /**
   * Converts any pojo (iuncluding Maps) to BsonDocument using the library custom CodecRegistry.
   */
  public static <T> BsonDocument toBsonDocument(T pojo) {
    BsonDocument unwrapped = new BsonDocument();
    BsonWriter writer = new BsonDocumentWriter(unwrapped);
    Codec<T> encoder = (Codec<T>) getCodecRegistry().get(pojo.getClass());
    encoder.encode(writer, pojo, EncoderContext.builder().build());
    return unwrapped;
  }

  public static BsonDocument toBsonDocument(String key, Object value, Object... keyAndValuePairs) {
    BsonDocument bsonDocument = new BsonDocument();
    bsonDocument.put(key, toBsonValue(value));

    if (keyAndValuePairs.length % 2 != 0) {
      throw new MongoPipeConfigException("Invalid BSON, missing value");
    }
    for (int i = 0; i< keyAndValuePairs.length; i++) {
      if (! (keyAndValuePairs[i] instanceof String)) {
        throw new MongoPipeConfigException("Invalid BSON");
      }
      bsonDocument.put((String)keyAndValuePairs[i], toBsonValue(keyAndValuePairs[++i]));
    }
    return bsonDocument;
  }

  public static BsonValue toBsonValue(Object value) {
    if (value == null) {
      return new BsonNull();
    } else if (value instanceof ObjectId) {
      return new BsonObjectId((ObjectId)value);
    } else if (value instanceof String) {
      return new BsonString(value.toString());
    } else if (value instanceof Integer) {
      return new BsonInt32((Integer) value);
    } else if (value instanceof Long) {
      return new BsonInt64((Long) value);
    } else if (value instanceof Boolean) {
      return new BsonBoolean((Boolean) value);
    } else if (value instanceof Double || value instanceof Float) {
      return new BsonDouble(Double.valueOf(value.toString()));
    } else if (value instanceof Map) {
      return toBsonDocument((Map)value);
    } else if (value instanceof List) {
      Map map = new HashMap<>();
      map.put("key", value);
      return toBsonDocument(value).get("key");
    } else {
      return toBsonDocument(value);
    }
  }

}
