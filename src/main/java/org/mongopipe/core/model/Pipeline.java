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

package org.mongopipe.core.model;

import org.bson.BsonDocument;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.conversions.Bson;
import org.mongopipe.core.runner.PipelineRunner;
import org.mongopipe.core.runner.command.param.CommandAndParams;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores both the pipeline and the running context (target collection, type of collection method and params).
 * NOTE: It does not store the actual pipeline variables values. Those are provided when the pipeline is actually executed:
 *  <code>pizzaRepository.getPizzas("Medium")</code> or <code>pipelineRunner.run("pizzaPipelineId", "Medium")</code>
 * You can create a Pipeline multiple ways:
 *   - pipelineStore.create
 *   - via classpath files
 */
public class Pipeline extends PipelineBase {
  /**
   * Uniquely identifies a pipeline run. Used by the @PipelineRun annotation in the pipeline repositories.
   */
  @BsonProperty("_id")
  String id;

  /**
   * The version will increase on each pipeline update performed by the org.mongopipe.core.migration or by an store update operation.
   */
  Long version;
  LocalDateTime insertedAt;
  LocalDateTime modifiedAt;

  /**
   * The raw/template BSON pipeline that is run by MongoDB. This contains pipeline variables in the form of ${variableName}. The values for
   * those variables are provided when the pipeline is run.
   * You can also provide it as a JSON string when calling the builder 'jsonPipeline' method but it will eventually be saved as a bson list.
   */
  List<BsonDocument> pipeline;

  /**
   * Optional string pipeline content. Has priority over bson {@link Pipeline#pipeline} when the pipeline is executed.
   * Used mainly because it is simpler to edit directly in the DB and many users will have easier direct access to DB than API.
   * It is automatically generated from the bson list on each create/update via the API.
   */
  String pipelineAsString;

  /**
   * Target collection on which to run.
   */
  String collection;
  String description;

  /**
   * Optionally fully qualified class name. This is not needed for pipeline repositories but only when running generically with
   * {@link PipelineRunner}.
   * Used to automatically map result to the specified type.
   * By default it is extracted from the @PipelineRun annotated method return type.
   * Can be also provided at runtime when running manually with PipelineRunner.
   */
  String resultClass;

  /**
   * Optionally the MongoDB command type and params that will use the pipeline. Defaulted to run 'db.collection.aggregate()'.
   * NOTE: These are NOT the same as the actual pipeline inline variables provided by the user on pipeline execution.
   */
  CommandAndParams commandAndParams;

  /**
   * Optionally extra JSON serializable information (e.g. array or object) that is <b>provided by user</b> and stored along with the
   * pipeline. E.g. Sample pipeline template parameter values for a hint on what values to provide for template variables.
   * TODO: Consider using @BsonExtraElements annotation. See https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/pojo-customization/
   */
  Serializable extra;

  public Pipeline() {
  }

  private Pipeline(Builder builder) {
    id = builder.id;
    version = builder.version;
    insertedAt = builder.insertedAt;
    modifiedAt = builder.modifiedAt;
    if (builder.pipeline != null) {
      pipeline = builder.pipeline.stream()
          .map(stage -> stage.toBsonDocument())
          .collect(Collectors.toList());
    }
    pipelineAsString = builder.pipelineAsString;
    collection = builder.collection;
    description = builder.description;
    resultClass = builder.resultClass;
    extra = builder.extra;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String id;
    private Long version;
    private LocalDateTime insertedAt;
    private LocalDateTime modifiedAt;
    private String pipelineAsString;
    private List<Bson> pipeline;
    private String collection;
    private String description;
    private String resultClass;
    private Serializable extra;

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

    public Builder pipeline(List<Bson> val) {
      pipeline = val;
      return this;
    }

    /**
     * The MongoDB raw pipeline in the form of a JSON or BSON string.
     */
    public Builder pipeline(String val) {
      pipelineAsString = val;
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

    public Builder extra(Serializable val) {
      extra = val;
      return this;
    }

    public Pipeline build() {
      return new Pipeline(this);
    }
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

  public String getPipelineAsString() {
    return pipelineAsString;
  }

  public void setPipelineAsString(String pipelineAsString) {
    this.pipelineAsString = pipelineAsString;
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

  public CommandAndParams getCommandAndParams() {
    return commandAndParams;
  }

  public <T extends CommandAndParams> T getCommandAndParamsAs(Class<T> clazz) {
    return (T)commandAndParams;
  }

  public void setCommandAndParams(CommandAndParams commandAndParams) {
    this.commandAndParams = commandAndParams;
  }

  public Serializable getExtra() {
    return extra;
  }

  public void setExtra(Serializable extra) {
    this.extra = extra;
  }
}
