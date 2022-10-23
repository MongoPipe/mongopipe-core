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

import org.bson.BsonDocument;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.mongopipe.core.util.BsonUtil.toBsonDocumentList;

/**
 * Stores both the pipeline and the running context (target collection, type of command, options).
 * In future the raw pipeline could be extracted and referenced
 * 
 */
public class PipelineRun {
  @BsonProperty("_id")
  String id;
  /**
   * The version will increase on each pipeline update performed by the org.mongopipe.core.migration or by an store update operation.
   */
  Long version;
  LocalDateTime insertedAt;
  LocalDateTime modifiedAt;
  /**
   * The raw/template BSON pipeline that is run by MongoDB.
   * You can also provide it as a JSON string when calling the builder 'jsonPipeline' method.
   */
  List<BsonDocument> pipeline;

  /**
   * Target collection on which to run.
   */
  String collection;
  String description;
  /**
   * Optionally fully qualified class name. Used to automatically map result to the specified type.
   * By default it is extracted from the @Pipeline annotated method return type.
   * Can be also provided at runtime when running manually with PipelineRunner.
   */
  String resultClass;

  /**
   * The pipeline command type. There are many commands that can use a pipeline. Specify here the operation to be executed.
   * Default is to run an 'db.collection.aggregate()'.
   */
  PipelineCommandType commandType;

  /**
   * The list of operation params excepting the pipeline, that are needed by the corresponding {@link #commandType} command.
   * E.g. for an aggregate pipeline: <code>{"allowDiskUse": true}</code>
   */
  List<Serializable> operationParams;

  /**
   * Optionally extra JSON serializable information (e.g. array or object) that is <b>provided by user</b> and stored along with the
   * pipeline. E.g. Sample pipeline template parameter values for a hint on what values to provide for template variables.
   * TODO: Consider using @BsonExtraElements annotation. See https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/pojo-customization/
   */
  Serializable notes;

  public PipelineRun() {
  }

  private PipelineRun(Builder builder) {
    setId(builder.id);
    setVersion(builder.version);
    setInsertedAt(builder.insertedAt);
    setModifiedAt(builder.modifiedAt);
    setPipeline(toBsonDocumentList(builder.jsonPipeline));
    setCollection(builder.collection);
    setDescription(builder.description);
    setResultClass(builder.resultClass);
    setCommandType(builder.commandType);
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

  public List<BsonDocument> getPipeline() {
    return pipeline;
  }

  public void setPipeline(List<BsonDocument> pipeline) {
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

  public PipelineCommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(PipelineCommandType commandType) {
    this.commandType = commandType;
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
    private String jsonPipeline;
    private String collection;
    private String description;
    private String resultClass;
    private PipelineCommandType commandType;
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

    public Builder jsonPipeline(String val) {
      jsonPipeline = val;
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

    public Builder resultClass(Class val) {
      resultClass = val.getCanonicalName();
      return this;
    }

    public Builder commandType(PipelineCommandType val) {
      commandType = val;
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
