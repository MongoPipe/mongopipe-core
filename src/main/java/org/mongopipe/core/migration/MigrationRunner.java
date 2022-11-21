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

import org.mongopipe.core.Stores;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.migration.model.MigrationStatus;
import org.mongopipe.core.migration.model.PipelineMigrationStatus;
import org.mongopipe.core.migration.model.Status;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.store.PipelineStore;
import org.mongopipe.core.store.StatusStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mongopipe.core.util.MigrationUtil.getChecksum;
import static org.mongopipe.core.util.MigrationUtil.getHash;

/**
 *
 *
 */
public class MigrationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(MigrationRunner.class);
  private final RunContext runContext;
  private final PipelineStore pipelineStore;
  private StatusStore statusStore;

  public MigrationRunner(RunContext runContext, PipelineStore pipelineStore) {
    this.runContext = runContext;
    this.pipelineStore = pipelineStore;
    statusStore = Stores.get(StatusStore.class);
  }

  public MigrationRunner() {
    this(RunContextProvider.getContext(), Stores.getPipelineStore());
  }

  public void run() {
    PipelineMigrationSource source = runContext.getPipelineMigrationSource();
    if (source == null) {
      throw new MongoPipeConfigException("Missing pipeline migration source");
    }
    // TODO: Consider allowing interface default methods in store interface, dig here https://stackoverflow.com/questions/37812393/how-to-explicitly-invoke-default-method-from-a-dynamic-proxy
    Optional<Status> statusOptional = statusStore.findById(1L);

    if (!statusOptional.isPresent()) {
      // First time migration runs.
      createAll(source);
    } else {
      // 1. First compute fast checksum of the source pipelines and compare with existing.
      //   If equal then no change needed. This means that existing db pipeline changes (done via API or directly), will remain untouched.
      // 2. If not equal then iterate on all units, compute checksum and compare with db one and if it differs then create/update.
      List<MigrablePipeline> migrablePipelines = source.getMigrablePipelines().collect(Collectors.toList());
      String sourceFastChecksum = getFastChecksum(migrablePipelines);
      Status status = statusOptional.get();

      String dbFastChecksum = status.getMigrationStatus().getFastChecksum();
      if(sourceFastChecksum.equals(dbFastChecksum)) {
        LOG.info("Pipeline migration not needed.");
      } else {
        List<PipelineMigrationStatus> pipelineMigrationStatuses = migrablePipelines.stream().map(migrablePipeline -> {

          Optional<PipelineMigrationStatus> dbStatus = status.getMigrationStatus().getPipelineMigrationStatuses().stream()
              .filter(migrationStatus -> migrationStatus.getPipelineId().equals(migrablePipeline.getPipeline().getId()))
              .findFirst();

          PipelineMigrationStatus pipelineMigrationStatus = null;
          if (!dbStatus.isPresent()) {
            pipelineMigrationStatus = savePipelineAndReturnMigrationStatus(migrablePipeline.getPipeline());
          } else {
            String migrableChecksum = getChecksum(migrablePipeline.getPipeline());
            String dbChecksum = dbStatus.get().getChecksum();
            if (migrableChecksum.equalsIgnoreCase(dbChecksum)) {
              LOG.debug("No migration needed for pipeline: " + migrablePipeline.getPipeline().getId());
              pipelineMigrationStatus = dbStatus.get();  // No change
            } else {
              pipelineMigrationStatus = savePipelineAndReturnMigrationStatus(migrablePipeline.getPipeline());
            }
          }
          return pipelineMigrationStatus;
        }).collect(Collectors.toList());

        saveNewStatus(migrablePipelines, pipelineMigrationStatuses);
      }
    }

    LOG.debug("Migration ended.");
  }

  private String getFastChecksum(List<MigrablePipeline> migrablePipelines) {
    return getHash(String.valueOf(migrablePipelines.stream()
        .map(unit -> "." + unit.getLastModifiedTime())
        .reduce("", String::concat)));
  }

  private void saveNewStatus(List<MigrablePipeline> migrablePipelines, List<PipelineMigrationStatus> pipelineMigrationStatuses) {
    LocalDateTime now = LocalDateTime.now();
    MigrationStatus migrationStatus = MigrationStatus.builder()
        .runAt(now)
        .build();
    migrationStatus.setPipelineMigrationStatuses(pipelineMigrationStatuses);
    migrationStatus.setFastChecksum(getFastChecksum(migrablePipelines));

    // Main status.
    Status status = Status.builder()
        .updatedAt(now)
        .migrationStatus(migrationStatus)
        .build();

    statusStore.save(status);
  }

  private void createAll(PipelineMigrationSource source) {
    List<MigrablePipeline> migrablePipelines = source.getMigrablePipelines().collect(Collectors.toList());
    List<PipelineMigrationStatus> pipelineMigrationStatuses = migrablePipelines.stream()
        .map(unit -> savePipelineAndReturnMigrationStatus(unit.getPipeline()))
        .collect(Collectors.toList());

    saveNewStatus(migrablePipelines, pipelineMigrationStatuses);
  }

  private PipelineMigrationStatus savePipelineAndReturnMigrationStatus(Pipeline pipeline) {
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
