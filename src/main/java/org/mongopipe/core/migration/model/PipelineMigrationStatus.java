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

public class PipelineMigrationStatus {
  String pipelineId;
  String checksum; // real checksum
  LocalDateTime updateTime;

  public PipelineMigrationStatus() {

  }

  public PipelineMigrationStatus(String pipelineId, String checksum, LocalDateTime updateTime) {
    this.pipelineId = pipelineId;
    this.checksum = checksum;
    this.updateTime = updateTime;
  }

  private PipelineMigrationStatus(Builder builder) {
    setPipelineId(builder.pipelineId);
    setChecksum(builder.checksum);
    setUpdateTime(builder.updateTime);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getPipelineId() {
    return pipelineId;
  }

  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public static final class Builder {
    private String pipelineId;
    private String checksum;
    private LocalDateTime updateTime;

    private Builder() {
    }

    public Builder pipelineId(String val) {
      pipelineId = val;
      return this;
    }

    public Builder checksum(String val) {
      checksum = val;
      return this;
    }

    public Builder updateTime(LocalDateTime val) {
      updateTime = val;
      return this;
    }

    public PipelineMigrationStatus build() {
      return new PipelineMigrationStatus(this);
    }
  }
}