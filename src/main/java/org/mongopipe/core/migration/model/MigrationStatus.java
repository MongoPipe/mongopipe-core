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

package org.mongopipe.core.migration.model;

import java.time.LocalDateTime;
import java.util.List;

public class MigrationStatus {
  LocalDateTime runAt;
  String fastChecksum;  // time based
  List<PipelineMigrationStatus> pipelineMigrationStatuses;

  private MigrationStatus(Builder builder) {
    setRunAt(builder.runAt);
    setFastChecksum(builder.fastChecksum);
    setPipelineMigrationStatuses(builder.pipelineMigrationStatuses);
  }

  public static Builder builder() {
    return new Builder();
  }

  public LocalDateTime getRunAt() {
    return runAt;
  }

  public void setRunAt(LocalDateTime runAt) {
    this.runAt = runAt;
  }

  public String getFastChecksum() {
    return fastChecksum;
  }

  public void setFastChecksum(String fastChecksum) {
    this.fastChecksum = fastChecksum;
  }

  public List<PipelineMigrationStatus> getPipelineMigrationStatuses() {
    return pipelineMigrationStatuses;
  }

  public void setPipelineMigrationStatuses(List<PipelineMigrationStatus> pipelineMigrationStatuses) {
    this.pipelineMigrationStatuses = pipelineMigrationStatuses;
  }

  public MigrationStatus() {
  }
  public MigrationStatus(LocalDateTime runAt, String fastChecksum, List<PipelineMigrationStatus> pipelineMigrationStatuses) {
    this.runAt = runAt;
    this.fastChecksum = fastChecksum;
    this.pipelineMigrationStatuses = pipelineMigrationStatuses;
  }

  public static final class Builder {
    private LocalDateTime runAt;
    private String fastChecksum;
    private List<PipelineMigrationStatus> pipelineMigrationStatuses;

    private Builder() {
    }

    public Builder runAt(LocalDateTime val) {
      runAt = val;
      return this;
    }

    public Builder fastChecksum(String val) {
      fastChecksum = val;
      return this;
    }

    public Builder pipelineMigrationStatuses(List<PipelineMigrationStatus> val) {
      pipelineMigrationStatuses = val;
      return this;
    }

    public MigrationStatus build() {
      return new MigrationStatus(this);
    }
  }
}
