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
import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.json.JsonReader;
import org.mongopipe.core.exception.MongoPipeConfigException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class BsonUtil {

  public static String toString(List<BsonDocument> bsonDocumentList) {
    return bsonDocumentList.toString();
  }

  public static List<BsonDocument> toBsonDocumentList(String bson) {
    // Document.parse(rawPipeline);
    // http://mongodb.github.io/mongo-java-driver/3.7/driver/getting-started/quick-start-pojo/
    // https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/document-data-format-pojo/
    // https://splunktool.com/jsonparse-equivalent-in-mongo-driver-3x-for-java
    // https://stackoverflow.com/questions/34436952/json-parse-equivalent-in-mongo-driver-3-x-for-java
    final CodecRegistry codecRegistry = CodecRegistries.fromProviders(Collections.singletonList(new BsonValueCodecProvider()));
    JsonReader reader = new JsonReader(bson);
    BsonArrayCodec arrayReader = new BsonArrayCodec(codecRegistry);
    BsonArray array = arrayReader.decode(reader, DecoderContext.builder().build());
    return array.stream()
        .map(BsonValue::asDocument)
        .collect(Collectors.toList());
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
      return toBsonDocumentList(bson).stream()
          .map(BsonUtil::toDocument)
          .collect(Collectors.toList());
    } catch (IOException | URISyntaxException e) {
      throw new MongoPipeConfigException("Can not load classpath file " + resourcePath, e);
    }
  }

  /**
   * Converts a String from a classpath resource to pojo class.
   */
  public static <T> T loadResourceIntoPojo(String resourcePath, Class<T> pojoClass) {
    try {
      // see bson2pojo https://stackoverflow.com/questions/71777864/how-to-convert-a-pojo-to-an-bson-using-mongodb-java-driver
      String bsonString = new String(Files.readAllBytes(Paths.get(BsonUtil.class.getClassLoader().getResource(resourcePath).toURI())));
      CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
          CodecRegistries.fromProviders(PojoCodecProvider.builder()
              .automatic(true)
              .build()));
      BsonDocument bsonDocument = Document.parse(bsonString).toBsonDocument();
      BsonReader reader = new BsonDocumentReader(bsonDocument);
      Decoder<T> decoder = pojoCodecRegistry.get(pojoClass);
      T pojo = decoder.decode(reader, DecoderContext.builder().build());
      return pojo;
    } catch (IOException | URISyntaxException e) {
      throw new MongoPipeConfigException("Can not load classpath file " + resourcePath, e);
    }
  }

}
