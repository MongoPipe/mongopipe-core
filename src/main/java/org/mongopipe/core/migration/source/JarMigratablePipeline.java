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

import lombok.CustomLog;
import org.mongopipe.core.exception.MongoPipeMigrationException;
import org.mongopipe.core.model.Pipeline;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;

import static org.mongopipe.core.util.BsonUtil.toPojo;

@CustomLog
public class JarMigratablePipeline implements MigratablePipeline {
  private Pipeline pipeline;
  private JarPipelineEntry jarPipelineEntry;

  public JarMigratablePipeline(JarPipelineEntry jarPipelineEntry) {
    this.jarPipelineEntry = jarPipelineEntry;
  }

  @Override
  public Long getLastModifiedTime() {
    return jarPipelineEntry.getLastModifiedTime();
  }

  @Override
  public Pipeline getPipeline() {
    if (pipeline == null) {
      // https://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory#comment67155595_3923685
      Reader reader = null;
      try {
        Optional<URL> pipelineUrlOptional = Collections.list(
            Thread.currentThread().getContextClassLoader().getResources(jarPipelineEntry.getPath())).stream()
            .filter(url -> url.getPath().contains(jarPipelineEntry.getJarPath()))
            .findFirst();
        if (!pipelineUrlOptional.isPresent()) {
          throw new MongoPipeMigrationException("Can not find pipeline for " + jarPipelineEntry);
        }
        reader = new InputStreamReader(pipelineUrlOptional.get().openStream(), Charset.defaultCharset().newDecoder());
        pipeline = toPojo(reader, Pipeline.class);
      } catch (IOException e) {
        throw new MongoPipeMigrationException(e);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            LOG.error(e.getMessage(), e);
          }
        }
      }
    }
    return pipeline;
  }

  @Override
  public String getSourceName() {
    return jarPipelineEntry.getJarPath();
  }
}
