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

import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;

/**
 * This keeps library configuration, migration configuration, etc.
 */
public class Status {
  public static final Long DEFAULT_ID = 1L;
  @BsonId
  Long id = DEFAULT_ID; // One and only.
  Long version;
  LocalDateTime updatedAt;
  MigrationStatus migrationStatus;

  public Status() {
  }

  public Status(Long id, Long version, LocalDateTime updatedAt, MigrationStatus migrationStatus) {
    this.id = id;
    this.version = version;
    this.updatedAt = updatedAt;
    this.migrationStatus = migrationStatus;
  }

  private Status(Builder builder) {
    id = builder.id;
    version = builder.version;
    updatedAt = builder.updatedAt;
    migrationStatus = builder.migrationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public MigrationStatus getMigrationStatus() {
    return migrationStatus;
  }

  public void setMigrationStatus(MigrationStatus migrationStatus) {
    this.migrationStatus = migrationStatus;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Long id = DEFAULT_ID;
    private Long version;
    private LocalDateTime updatedAt;
    private MigrationStatus migrationStatus;

    private Builder() {
    }

    public Builder id(Long val) {
      id = val;
      return this;
    }

    public Builder version(Long val) {
      version = val;
      return this;
    }

    public Builder updatedAt(LocalDateTime val) {
      updatedAt = val;
      return this;
    }

    public Builder migrationStatus(MigrationStatus val) {
      migrationStatus = val;
      return this;
    }

    public Status build() {
      return new Status(this);
    }
  }
}
