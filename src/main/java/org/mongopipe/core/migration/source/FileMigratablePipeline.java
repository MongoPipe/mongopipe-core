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

package org.mongopipe.core.migration.source;

import org.mongopipe.core.exception.MongoPipeMigrationException;
import org.mongopipe.core.model.Pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;

import static org.mongopipe.core.util.BsonUtil.toPojo;

public class FileMigratablePipeline implements MigratablePipeline {

  private File file;
  private Pipeline pipeline;

  public FileMigratablePipeline(File file) {
    this.file = file;
  }

  @Override
  public Long getLastModifiedTime() {
    return file.lastModified();
  }

  @Override
  public Pipeline getPipeline() {
    if (pipeline == null) {
      // Alternative:
      // pipeline = BsonUtil.loadResourceIntoPojo(this.resourcePath, Pipeline.class);
      // https://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory#comment67155595_3923685
      try {
        //String content = new String(Files.readAllBytes(file.toPath()));
        pipeline = toPojo(Channels.newReader(FileChannel.open(file.toPath(), StandardOpenOption.READ),
            Charset.defaultCharset().newDecoder(), 4096), Pipeline.class);
      } catch (IOException e) {
        throw new MongoPipeMigrationException("Could not read pipeline file " + file.getAbsolutePath(), e);
      }
    }
    return pipeline;
  }

  @Override
  public String getSourceName() {
    return file.getAbsolutePath();
  }
}
