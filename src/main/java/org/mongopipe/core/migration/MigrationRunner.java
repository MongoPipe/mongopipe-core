/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
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

package org.mongopipe.core.migration;

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.config.MigrationConfig;
import org.mongopipe.core.exception.MongoPipeMigrationException;
import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;
import org.mongopipe.core.migration.model.MigrationStatus;
import org.mongopipe.core.migration.model.PipelineMigrationStatus;
import org.mongopipe.core.migration.model.Status;
import org.mongopipe.core.migration.source.MigratablePipeline;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.store.PipelineStore;
import org.mongopipe.core.store.StatusStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mongopipe.core.util.MigrationUtil.getChecksum;
import static org.mongopipe.core.util.MigrationUtil.getHash;

/**
 * Runs the pipeline migration from a given source usually a resources folder.
 * This will allow the automated creation/update of pipelines on process startup.
 */
public class MigrationRunner {
  private static final Log LOG = CustomLogFactory.getLogger(MigrationRunner.class);
  private final RunContext runContext;
  private final PipelineStore pipelineStore;
  private StatusStore statusStore;

  public MigrationRunner(RunContext runContext, PipelineStore pipelineStore) {
    this.runContext = runContext;
    this.pipelineStore = pipelineStore;
    statusStore = Stores.from(StatusStore.class);
  }

  public MigrationRunner() {
    this(RunContextProvider.getContext(), Stores.getPipelineStore());
  }

  public void run() {
    MigrationConfig migrationConfig = runContext.getMongoPipeConfig().getMigrationConfig();
    if (!migrationConfig.isEnabled()) {
      LOG.warn("Skip pipeline migration.");
      return;
    }
    PipelineMigrationSource source = runContext.getPipelineMigrationSource();

    if (source == null) {
      throw new MongoPipeMigrationException("Missing pipeline migration source");
    }
    Optional<Status> statusOptional = statusStore.getStatus();

    if (!statusOptional.isPresent()) {
      // First time migration runs.
      createAll(source);
    } else {
      // 1. First compute fast checksum of the source pipelines and compare with existing.
      //   If equal then no change needed. This means that existing db pipeline changes (done via API or directly), will remain untouched.
      // 2. If not equal then iterate on all units, compute checksum and compare with db one and if it differs then create/update.
      List<MigratablePipeline> migratablePipelines = source.getMigrablePipelines().collect(Collectors.toList());
      String sourceFastChecksum = getFastChecksum(migratablePipelines);
      Status status = statusOptional.get();

      String dbFastChecksum = status.getMigrationStatus().getFastChecksum();
      if(sourceFastChecksum.equals(dbFastChecksum)) {
        LOG.info("Pipeline migration not needed.");
      } else {
        List<PipelineMigrationStatus> pipelineMigrationStatuses = migratablePipelines.stream().map(migrablePipeline -> {

          Optional<PipelineMigrationStatus> dbStatus = status.getMigrationStatus().getPipelineMigrationStatuses().stream()
              .filter(migrationStatus -> migrationStatus.getPipelineId().equals(migrablePipeline.getPipeline().getId()))
              .findFirst();

          PipelineMigrationStatus pipelineMigrationStatus = null;
          if (!dbStatus.isPresent()) {
            pipelineMigrationStatus = savePipelineAndReturnMigrationStatus(migrablePipeline);
          } else {
            String migrableChecksum = getChecksum(migrablePipeline.getPipeline());
            String dbChecksum = dbStatus.get().getChecksum();
            if (migrableChecksum.equalsIgnoreCase(dbChecksum)) {
              LOG.debug("No migration needed for pipeline: " + migrablePipeline.getPipeline().getId());
              pipelineMigrationStatus = dbStatus.get();  // No change
            } else {
              pipelineMigrationStatus = savePipelineAndReturnMigrationStatus(migrablePipeline);
            }
          }
          return pipelineMigrationStatus;
        }).collect(Collectors.toList());

        saveNewStatus(migratablePipelines, pipelineMigrationStatuses);
      }
    }

    LOG.debug("Migration ended.");
  }

  private String getFastChecksum(List<MigratablePipeline> migratablePipelines) {
    return getHash(String.valueOf(migratablePipelines.stream()
        .map(unit -> "." + unit.getLastModifiedTime())
        .reduce("", String::concat)));
  }

  private void saveNewStatus(List<MigratablePipeline> migratablePipelines, List<PipelineMigrationStatus> pipelineMigrationStatuses) {
    LocalDateTime now = LocalDateTime.now();
    MigrationStatus migrationStatus = MigrationStatus.builder()
        .runAt(now)
        .build();
    migrationStatus.setPipelineMigrationStatuses(pipelineMigrationStatuses);
    migrationStatus.setFastChecksum(getFastChecksum(migratablePipelines));

    // Main status.
    Status status = Status.builder()
        .updatedAt(now)
        .migrationStatus(migrationStatus)
        .build();

    statusStore.save(status);
  }

  private void createAll(PipelineMigrationSource source) {
    List<MigratablePipeline> migratablePipelines = source.getMigrablePipelines().collect(Collectors.toList());
    List<PipelineMigrationStatus> pipelineMigrationStatuses = migratablePipelines.stream()
        .map(unit -> savePipelineAndReturnMigrationStatus(unit))
        .collect(Collectors.toList());

    saveNewStatus(migratablePipelines, pipelineMigrationStatuses);
  }

  private PipelineMigrationStatus savePipelineAndReturnMigrationStatus(MigratablePipeline migratablePipeline) {
    Pipeline pipeline = migratablePipeline.getPipeline();
    LOG.info("Updating pipeline {} from source {}", pipeline.getId(), migratablePipeline.getSourceName());
    try {
      pipeline = pipelineStore.update(pipeline);
    } catch (Exception e) {
      LOG.error("Skip pipeline migration for '" + pipeline.getId() + "'. Exception:" + e.getMessage(), e);
    }
    return PipelineMigrationStatus.builder()
        .pipelineId(pipeline.getId())
        .checksum(getChecksum(pipeline))
        .updateTime(pipeline.getUpdatedAt())
        .build();
  }

}
