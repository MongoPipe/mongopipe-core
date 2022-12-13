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

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.config.MigrationConfig;
import org.mongopipe.core.exception.MongoPipeMigrationException;
import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;
import org.mongopipe.core.migration.source.FileMigratablePipelineScanner;
import org.mongopipe.core.migration.source.JarMigratablePipelineScanner;
import org.mongopipe.core.migration.source.MigratablePipeline;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

public class PipelineMigrationSourceImpl implements PipelineMigrationSource {
  private static final Log LOG = CustomLogFactory.getLogger(PipelineMigrationSourceImpl.class);
  MigrationConfig migrationConfig;
  FileMigratablePipelineScanner fileMigratablePipelineScanner = new FileMigratablePipelineScanner();
  JarMigratablePipelineScanner jarMigratablePipelineScanner = new JarMigratablePipelineScanner();

  public PipelineMigrationSourceImpl(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }


  private List<MigratablePipeline> getPipelineSources() {
    try {
      if (migrationConfig.getPipelinesPath().endsWith("/")) {
        migrationConfig.setPipelinesPath(migrationConfig.getPipelinesPath() + "/");
      }
      // Gives URLs from all classpath jars including file sources. E.g. jar:file:/path/.../lib.jar!/folderName or file:/path/.../folderName
      Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(migrationConfig.getPipelinesPath());

      List<MigratablePipeline> migratablePipelines = new ArrayList<>();
      Collections.list(resources).stream()
          .forEach(url -> {
            migratablePipelines.addAll(fileMigratablePipelineScanner.loadPipelinesFromLocation(url, migrationConfig));
            migratablePipelines.addAll(jarMigratablePipelineScanner.loadPipelinesFromLocation(url, migrationConfig));
          });

      if (migratablePipelines.size() == 0) {
        LOG.warn("No pipelines available for migration in path: ", migrationConfig.getPipelinesPath());
      }
      return migratablePipelines;

    } catch (IOException e) {
      throw new MongoPipeMigrationException("Could not load pipeline source:" + e.getMessage(), e);
    }
  }

  @Override
  public Stream<MigratablePipeline> getMigrablePipelines() {
    return getPipelineSources().stream();
  }


}
