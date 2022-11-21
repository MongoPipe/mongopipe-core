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

import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MigrationUtil {
  private static MessageDigest DIGEST;
  static {
    try {
      DIGEST = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new MongoPipeConfigException(e.getMessage(), e);
    }
  }

  public static String getHash(String text) {
    // text.hashCode() has some small chance of collisions.
    byte[] hash = DIGEST.digest(text.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hash);
  }

  public static String getChecksum(Pipeline pipeline) {
    return getHash(BsonUtil.toBsonDocument(pipeline).toJson());
  }
}
