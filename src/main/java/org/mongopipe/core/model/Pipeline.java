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
import org.bson.conversions.Bson;
import org.mongopipe.core.runner.command.param.CommandOptions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.mongopipe.core.util.BsonUtil.toBsonList;

/**
 * Stores both the pipeline and the running context (target collection, type of collection method and params).
 * Pipeline stages <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/">documentation</a>.
 *
 * NOTES:
 * It does not store the actual pipeline variables values. Those are provided when the pipeline is actually executed:
 *  <code>pizzaStore.getPizzas("Medium")</code> or <code>pipelineRunner.run("pizzaPipelineId", "Medium")</code>
 * No return type of the pipeline is provided since that would tie to implementation and restrict refactoring.
 *
 * User provided variables/parameters are String values similar with <code>{... field: "${paramName}", ...}</code> but will get replaced
 * with the actual type (number, string, object, array) of the value provided by the user when the pipeline is run.
 *
 * You can create a Pipeline multiple ways:
 *    1. Manually using <pre>Stores.getPipelineStore().create</pre>
 *    2. Via classpath files (created/updated automatically by Migration flow). See documentation site.
 * Depending on your use case both ways are helpful.
 *
 * Manual creation:
 * <pre>
 *     // Use PipelineStore for any CRUD operations on pipelines.
 *     PipelineStore pipelineStore = Stores.getPipelineStore();
 *
 *     // 1. From a String:
 *     String bsonStringPipeline = "{ \"id\": \"myPipeline\", \"collection\": \"pizzaCollection\", \"pipeline\": [ ...";
 *     Pipeline pipeline = BsonUtil.toPojo(bsonString, Pipeline.class);
 *     pipelineStore.createPipeline(pipeline);
 *
 *     // 2. Dynamically using BSON API, static imports are from Mongo driver API class: com.mongodb.client.model.Aggregates / Filters.
 *     Bson matchStage = match(and(eq("size", "$size"), eq("available", "$available")));
 *     Bson sortByCountStage = sort(descending("price"));
 *     pipelineStore.createPipeline(Pipeline.builder()
 *         .id("myPipeline")
 *         .pipeline(asList(matchStage, sortByCountStage))
 *         //.pipelineAsString("...") can be also provided as a string
 *         .collection("testCollection")
 *         .build());
 * </pre>
 *
 */
public class Pipeline extends MongoEntity {
  /**
   * The version will increase on each pipeline update performed by the org.mongopipe.core.migration or by an store update operation.
   */
  Long version;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;

  /**
   * Stores all the pipeline stages as a BSON array.

   * Pipeline stages <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/">documentation</a>.
   * The raw/template BSON pipeline that is run by MongoDB. This contains pipeline variables in the form of ${variableName}. The values for
   * those variables are provided when the pipeline is run.
   * You can also provide it as a JSON string when calling the builder 'jsonPipeline' method but it will eventually be saved as a bson list.
   */
  List<BsonDocument> pipeline;

  /**
   * Stores all the pipeline stages as a String. This is by default a automatically generated copy of the Pipeline#pipeline field but if it
   * is provided by the user then it will take precedence.
   * The main purpose of it is to quickly be able to send the pipeline as a String from
   * Pipeline stages <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/">documentation</a>.
   * Optional string pipeline content. Has priority over bson {@link Pipeline#pipeline} when the pipeline is executed.
   * Used mainly because it is simpler to edit directly in the DB and many users will have easier direct access to DB than API.
   * It is automatically generated from the bson list on each create/update via the API.
   */
  String pipelineAsString = "[]";

  /**
   * Target collection on which to run.
   */
  String collection;
  String description;

  /**
   * Optionally the MongoDB command type and params that will use the pipeline. Defaulted to run 'db.collection.aggregate()'.
   * NOTE: These are NOT the same as the actual pipeline inline variables provided by the user on pipeline execution.
   */
  CommandOptions commandOptions;

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
    createdAt = builder.insertedAt;
    updatedAt = builder.modifiedAt;
    if (builder.pipelineAsString != null) {
      pipeline = toBsonList(builder.pipelineAsString);
    }
    if (builder.pipeline != null) {
      pipeline = builder.pipeline.stream()
          .map(stage -> stage.toBsonDocument())
          .collect(Collectors.toList());
    }
    pipelineAsString = builder.pipelineAsString != null ? builder.pipelineAsString : pipelineAsString;
    collection = builder.collection;
    description = builder.description;
    commandOptions = builder.commandOptions;
    extra = builder.extra;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String id;
    private Long version = 1L;
    private LocalDateTime insertedAt;
    private LocalDateTime modifiedAt;
    private String pipelineAsString;
    private List<Bson> pipeline;
    private String collection;
    private String description;
    private String resultClass;
    private Serializable extra;
    private CommandOptions commandOptions;

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

    public Builder resultClass(Class val) {
      resultClass = val.getCanonicalName();
      return this;
    }

    public Builder extra(Serializable val) {
      extra = val;
      return this;
    }

    public Builder commandOptions(CommandOptions val) {
      commandOptions = val;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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

  public CommandOptions getCommandOptions() {
    return commandOptions;
  }

  public <T extends CommandOptions> T getCommandOptionsAs(Class<T> clazz) {
    return (T) commandOptions;
  }

  public void setCommandOptions(CommandOptions commandOptions) {
    this.commandOptions = commandOptions;
  }

  public Serializable getExtra() {
    return extra;
  }

  public void setExtra(Serializable extra) {
    this.extra = extra;
  }

  @Override
  public java.lang.String toString() {
    return "Pipeline(version="
        + this.getVersion()
        + ", createdAt="
        + this.getCreatedAt()
        + ", updatedAt="
        + this.getUpdatedAt()
        + ", pipeline="
        + this.getPipeline()
        + ", pipelineAsString="
        + this.getPipelineAsString()
        + ", collection="
        + this.getCollection()
        + ", description="
        + this.getDescription()
        + ", commandOptions="
        + this.getCommandOptions()
        + ", extra="
        + this.getExtra()
        + ")";
  }
}
