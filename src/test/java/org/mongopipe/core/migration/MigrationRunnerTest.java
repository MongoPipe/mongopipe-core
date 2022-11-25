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

import lombok.Builder;
import lombok.Data;
import org.junit.Test;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.Stores;
import org.mongopipe.core.migration.model.MigrationStatus;
import org.mongopipe.core.migration.model.PipelineMigrationStatus;
import org.mongopipe.core.migration.model.Status;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.context.RunContextProvider;
import org.mongopipe.core.store.PipelineStore;
import org.mongopipe.core.store.StatusStore;
import org.mongopipe.core.util.AbstractMongoDBTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotEquals;
import static org.mongopipe.core.util.BsonUtil.loadResourceIntoPojo;
import static org.mongopipe.core.util.MigrationUtil.getChecksum;

public class MigrationRunnerTest extends AbstractMongoDBTest {

  @Builder
  @Data
  static class TestMigrablePipeline implements MigrablePipeline {
    private final Long lastModifiedTime;
    private final Pipeline pipeline;

    public TestMigrablePipeline(Long lastModifiedTime, Pipeline pipeline) {
      this.lastModifiedTime = lastModifiedTime;
      this.pipeline = pipeline;
    }

    @Override
    public Long getLastModifiedTime() {
      return lastModifiedTime;
    }

    @Override
    public Pipeline getPipeline() {
      return pipeline;
    }
  }

  @Test
  public void testCreateAll() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/aggregate/pipeline.bson", Pipeline.class);
    Pipeline pipeline2 = loadResourceIntoPojo("command/aggregate/pipelinePizzasByPrice.bson", Pipeline.class);
    Stream<MigrablePipeline> migrationUnitStream = Arrays.asList(new MigrablePipeline[] {
        TestMigrablePipeline.builder().lastModifiedTime(1L).pipeline(pipeline).build(),
        TestMigrablePipeline.builder().lastModifiedTime(1L).pipeline(pipeline2).build()
    }).stream();
    RunContextProvider.getContext().setPipelineMigrationSource(() -> migrationUnitStream);

    // When
    new MigrationRunner().run();

    // Then
    assertEquals(Long.valueOf(2), Stores.getPipelineStore().count());
    Status status = Stores.get(StatusStore.class).findById(1L).get();
    assertNotNull(status.getUpdatedAt());
    MigrationStatus migrationStatus = status.getMigrationStatus();
    assertNotNull(migrationStatus.getFastChecksum());
    assertEquals(2, migrationStatus.getPipelineMigrationStatuses().size());
    PipelineMigrationStatus pipelineMigrationStatus = migrationStatus.getPipelineMigrationStatuses().get(0);
    assertEquals(pipeline.getId(), pipelineMigrationStatus.getPipelineId());
    assertNotNull(pipelineMigrationStatus.getChecksum());
    assertNotNull(pipelineMigrationStatus.getUpdateTime());

  }

  @Test
  public void testNoMigrationHappensWhenFastChecksumMatches() {
    // Given
    Pipeline pipeline = loadResourceIntoPojo("command/aggregate/pipeline.bson", Pipeline.class);
    Pipeline pipeline2 = loadResourceIntoPojo("command/aggregate/pipelinePizzasByPrice.bson", Pipeline.class);
    Stream<MigrablePipeline> migrationUnitStream = Arrays.asList(new MigrablePipeline[] {
        TestMigrablePipeline.builder().lastModifiedTime(1L).pipeline(pipeline).build(),
        TestMigrablePipeline.builder().lastModifiedTime(2L).pipeline(pipeline2).build()
    }).stream();
    RunContextProvider.getContext().setPipelineMigrationSource(() -> migrationUnitStream);

    StatusStore statusStore = Stores.get(StatusStore.class);
    statusStore.save(Status.builder()
        .migrationStatus(MigrationStatus.builder().fastChecksum("06CemFwL4MdybFK0166GTQ9DsXkcqMqZBmpD2B39FMU=").build())
        .build());

    // When
    new MigrationRunner().run();

    // Then
    assertEquals(Long.valueOf(0L), Stores.getPipelineStore().count());
  }

  @Test
  public void testMigrationHappensWhenFastChecksumNotEqualAndThatOnlyDifferentOrNewPipelinesAreUpdated() {
    // Given
    Pipeline pipeline1 = loadResourceIntoPojo("command/aggregate/pipeline.bson", Pipeline.class);
    Pipeline pipeline2WillBeUpdated = loadResourceIntoPojo("command/aggregate/pipelinePizzasByPrice.bson", Pipeline.class);
    Pipeline pipeline3IsNew = loadResourceIntoPojo("command/findOneAndUpdate/updateOneMatchingPizza.bson", Pipeline.class);

    // Incoming migration source.
    Pipeline pipeline2Updated = loadResourceIntoPojo("command/aggregate/pipelinePizzasByPrice.bson", Pipeline.class);
    pipeline2Updated.setDescription("pipeline2Updated");
    Stream<MigrablePipeline> migrationUnitStream = Arrays.asList(new MigrablePipeline[] {
        TestMigrablePipeline.builder().lastModifiedTime(1L).pipeline(pipeline1).build(),
        TestMigrablePipeline.builder().lastModifiedTime(2L).pipeline(pipeline2Updated).build(),
        TestMigrablePipeline.builder().lastModifiedTime(3L).pipeline(pipeline3IsNew).build()
    }).stream();
    RunContextProvider.getContext().setPipelineMigrationSource(() -> migrationUnitStream);


    List<PipelineMigrationStatus> dbStatuses = new ArrayList<>();
    PipelineStore pipelineStore = Stores.getPipelineStore();
    // First db pipeline.
    LocalDateTime pipeline1UpdateTime = LocalDateTime.now();
    pipeline1 = pipelineStore.create(pipeline1);
    dbStatuses.add(PipelineMigrationStatus.builder()
        .pipelineId(pipeline1.getId())
        .checksum(getChecksum(pipeline1))
        .updateTime(pipeline1UpdateTime)
        .build()
    );
    // Second db pipeline.
    pipelineStore.create(pipeline2WillBeUpdated);
    dbStatuses.add(PipelineMigrationStatus.builder()
        .pipelineId(pipeline2WillBeUpdated.getId())
        .checksum("NON MATCHING CHECKSUM")
        .build()
    );
    // No third pipeline is saved in db, because the third one will come from migration.

    // Save existing migration status.
    StatusStore statusStore = Stores.get(StatusStore.class);
    statusStore.save(Status.builder()
        .migrationStatus(MigrationStatus.builder()
            .fastChecksum("not matching")
            .pipelineMigrationStatuses(dbStatuses)
            .build())
        .build());


    // When
    Pipelines.startMigration();

    // Then
    assertEquals(Long.valueOf(3L), pipelineStore.count());

    assertNotNull(pipelineStore.getPipeline(pipeline3IsNew.getId()));
    // Pipeline 2 got updated by migration.
    assertEquals("pipeline2Updated", pipelineStore.getPipeline(pipeline2WillBeUpdated.getId()).getDescription());
    assertNotEquals(pipeline2WillBeUpdated.getDescription(), pipelineStore.getPipeline(pipeline2WillBeUpdated.getId()).getDescription());

    Status newStatus = Stores.get(StatusStore.class).findById(1L).get();
    assertNotNull(newStatus.getUpdatedAt());
    assertEquals("DzlfWW+ikyM+XEnMxBUsluH0L3tE/ArBzFrsc+H2oTA=", newStatus.getMigrationStatus().getFastChecksum());
    List<PipelineMigrationStatus> pipelineMigrationStatuses = newStatus.getMigrationStatus().getPipelineMigrationStatuses();

    assertEquals(pipeline1.getId(), pipelineMigrationStatuses.get(0).getPipelineId());
    assertEquals(getChecksum(pipeline1), pipelineMigrationStatuses.get(0).getChecksum());
    assertEquals(pipeline1UpdateTime, pipelineMigrationStatuses.get(0).getUpdateTime());

    assertEquals(pipeline2WillBeUpdated.getId(), pipelineMigrationStatuses.get(1).getPipelineId());
    String pipeline2Checksum = getChecksum(Stores.getPipelineStore().getPipeline(pipeline2WillBeUpdated.getId()));
    assertEquals(pipeline2Checksum, pipelineMigrationStatuses.get(1).getChecksum());
    assertNotNull(pipelineMigrationStatuses.get(1).getUpdateTime());

    assertEquals(pipeline3IsNew.getId(), pipelineMigrationStatuses.get(2).getPipelineId());
    String pipeline3Checksum = getChecksum(Stores.getPipelineStore().getPipeline(pipeline3IsNew.getId()));
    assertEquals(pipeline3Checksum, pipelineMigrationStatuses.get(2).getChecksum());
    assertNotNull(pipelineMigrationStatuses.get(2).getUpdateTime());

  }
}
