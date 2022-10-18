/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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

package org.mongopipe.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class PipelineRun {
  String id;
  /**
   * The version will increase on each pipeline update performed by the org.mongopipe.core.migration or by an store update operation.
   */
  Long version;
  LocalDateTime insertedAt;
  LocalDateTime modifiedAt;
  /**
   * The raw/template BSON pipeline that is run by MongoDB
   */
  String pipeline;
  /**
   * Target collection on which to run.
   */
  String collection;
  String description;
  /**
   * Optionally fully qualified class name. Used to automatically map result to the specified type. Can be also provided
   * at runtime when running.
   * If missing then the result will be of generic type, i.e. Map, List, String, Integer, etc.
   */
  String resultClass;

  /**
   * The pipeline operation type. There are many commands that can use a pipeline. Specify here the operation to be executed.
   * Default is to run an 'db.collection.aggregate()'. Store it here to avoid complication of the call API.
   */
  PipelineOperationType operationType;

  /**
   * The list of operation params excepting the pipeline, that are needed by the corresponding {@link #operationType} command.
   * E.g. for an aggregate pipeline: <code>{"allowDiskUse": true}</code>
   */
  List<Serializable> operationParams;

  /**
   * Optionally extra JSON serializable information (e.g. array or object) that is <b>provided by user</b> and stored along with the
   * pipeline. E.g. Sample pipeline template parameter values for a hint on what values to provide for template variables.
   */
  Serializable notes;

  private PipelineRun(Builder builder) {
    setId(builder.id);
    setVersion(builder.version);
    setInsertedAt(builder.insertedAt);
    setModifiedAt(builder.modifiedAt);
    setPipeline(builder.pipeline);
    setCollection(builder.collection);
    setDescription(builder.description);
    setResultClass(builder.resultClass);
    setOperationType(builder.operationType);
    setOperationParams(builder.operationParams);
    setNotes(builder.notes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public LocalDateTime getInsertedAt() {
    return insertedAt;
  }

  public void setInsertedAt(LocalDateTime insertedAt) {
    this.insertedAt = insertedAt;
  }

  public LocalDateTime getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(LocalDateTime modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public String getPipeline() {
    return pipeline;
  }

  public void setPipeline(String pipeline) {
    this.pipeline = pipeline;
  }

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getResultClass() {
    return resultClass;
  }

  public void setResultClass(String resultClass) {
    this.resultClass = resultClass;
  }

  public PipelineOperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(PipelineOperationType operationType) {
    this.operationType = operationType;
  }

  public List<Serializable> getOperationParams() {
    return operationParams;
  }

  public void setOperationParams(List<Serializable> operationParams) {
    this.operationParams = operationParams;
  }

  public Serializable getNotes() {
    return notes;
  }

  public void setNotes(Serializable notes) {
    this.notes = notes;
  }


  public static final class Builder {
    private String id;
    private Long version;
    private LocalDateTime insertedAt;
    private LocalDateTime modifiedAt;
    private String pipeline;
    private String collection;
    private String description;
    private String resultClass;
    private PipelineOperationType operationType;
    private List<Serializable> operationParams;
    private Serializable notes;

    private Builder() {
    }

    public Builder id(String val) {
      id = val;
      return this;
    }

    public Builder version(Long val) {
      version = val;
      return this;
    }

    public Builder insertedAt(LocalDateTime val) {
      insertedAt = val;
      return this;
    }

    public Builder modifiedAt(LocalDateTime val) {
      modifiedAt = val;
      return this;
    }

    public Builder pipeline(String val) {
      pipeline = val;
      return this;
    }

    public Builder collection(String val) {
      collection = val;
      return this;
    }

    public Builder description(String val) {
      description = val;
      return this;
    }

    public Builder resultClass(String val) {
      resultClass = val;
      return this;
    }

    public Builder operationType(PipelineOperationType val) {
      operationType = val;
      return this;
    }

    public Builder operationParams(List<Serializable> val) {
      operationParams = val;
      return this;
    }

    public Builder notes(Serializable val) {
      notes = val;
      return this;
    }

    public PipelineRun build() {
      return new PipelineRun(this);
    }
  }
}
