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

package org.mongopipe.core.migration;

import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.util.BsonUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class ClasspathMigrablePipeline implements MigrablePipeline {

  private final String resourcePath;
  private Pipeline pipeline;
  private long lastModifiedTime;

  public ClasspathMigrablePipeline(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  @Override
  public Long getLastModifiedTime() {
    try {
      Path filePath = BsonUtil.getPathFromResource(this.resourcePath);
      lastModifiedTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastModifiedTime().toMillis();
    } catch (URISyntaxException | IOException exception) {
      throw new MongoPipeConfigException("Resource file path not valid or not exist: " + this.resourcePath, exception);
    }

    return lastModifiedTime;
  }

  @Override
  public Pipeline getPipeline() {
    // TODO: Lazily load using  BsonUtil.loadResourceIntoPojo(String resourcePath, Class<T> pojoClass)
    // This means that the MigrationRunner will not need
    // And consider setting the value of lastModifiedTime if we also read the file.
    pipeline = BsonUtil.loadResourceIntoPojo(this.resourcePath, Pipeline.class);
    return pipeline;
  }
}
