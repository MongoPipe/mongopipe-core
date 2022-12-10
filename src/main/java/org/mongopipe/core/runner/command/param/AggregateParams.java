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

package org.mongopipe.core.runner.command.param;

import com.mongodb.ExplainVerbosity;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import static org.mongopipe.core.runner.command.param.CommandOptions.TYPE_KEY;

/**
 * Stores parameters for <a href="https://www.mongodb.com/docs/manual/reference/method/db.collection.aggregate/">aggregate</a>.
 */
@BsonDiscriminator(value = AggregateParams.TYPE, key = TYPE_KEY)
public class AggregateParams extends CommandOptions {
  public static final String TYPE = "aggregate";
  private final String type = TYPE;
  private Boolean allowDiskUse;
  private String comment;
  private String hint;
  private Integer batchSize;
  private Boolean bypassDocumentValidation;
  private ExplainVerbosity explainVerbosity;
  private Long maxTimeMS;
  private Long maxAwaitTime;
  private Collation collation;
  private Document let;

  public AggregateParams() {
  }

  private AggregateParams(Builder builder) {
    setAllowDiskUse(builder.allowDiskUse);
    setComment(builder.comment);
    setHint(builder.hint);
    setBatchSize(builder.batchSize);
    setBypassDocumentValidation(builder.bypassDocumentValidation);
    setExplainVerbosity(builder.explainVerbosity);
    setMaxTimeMS(builder.maxTimeMS);
    setMaxAwaitTime(builder.maxAwaitTime);
    setCollation(builder.collation);
    setLet(builder.let);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Boolean getAllowDiskUse() {
    return allowDiskUse;
  }

  public void setAllowDiskUse(Boolean allowDiskUse) {
    this.allowDiskUse = allowDiskUse;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public Boolean getBypassDocumentValidation() {
    return bypassDocumentValidation;
  }

  public void setBypassDocumentValidation(Boolean bypassDocumentValidation) {
    this.bypassDocumentValidation = bypassDocumentValidation;
  }

  public ExplainVerbosity getExplainVerbosity() {
    return explainVerbosity;
  }

  public void setExplainVerbosity(ExplainVerbosity explainVerbosity) {
    this.explainVerbosity = explainVerbosity;
  }

  public Long getMaxTimeMS() {
    return maxTimeMS;
  }

  public void setMaxTimeMS(Long maxTimeMS) {
    this.maxTimeMS = maxTimeMS;
  }

  public Long getMaxAwaitTime() {
    return maxAwaitTime;
  }

  public void setMaxAwaitTime(Long maxAwaitTime) {
    this.maxAwaitTime = maxAwaitTime;
  }

  public Collation getCollation() {
    return collation;
  }

  public void setCollation(Collation collation) {
    this.collation = collation;
  }

  public Document getLet() {
    return let;
  }

  public void setLet(Document let) {
    this.let = let;
  }

  @Override
  public String getType() {
    return type;
  }

  public static final class Builder {
    private Boolean allowDiskUse;
    private String comment;
    private String hint;
    private Integer batchSize;
    private Boolean bypassDocumentValidation;
    private ExplainVerbosity explainVerbosity;
    private Long maxTimeMS;
    private Long maxAwaitTime;
    private Collation collation;
    private Document let;

    private Builder() {
    }

    public Builder allowDiskUse(Boolean val) {
      allowDiskUse = val;
      return this;
    }

    public Builder comment(String val) {
      comment = val;
      return this;
    }

    public Builder hint(String val) {
      hint = val;
      return this;
    }

    public Builder batchSize(Integer val) {
      batchSize = val;
      return this;
    }

    public Builder bypassDocumentValidation(Boolean val) {
      bypassDocumentValidation = val;
      return this;
    }

    public Builder explainVerbosity(ExplainVerbosity val) {
      explainVerbosity = val;
      return this;
    }

    public Builder maxTimeMS(Long val) {
      maxTimeMS = val;
      return this;
    }

    public Builder maxAwaitTime(Long val) {
      maxAwaitTime = val;
      return this;
    }

    public Builder collation(Collation val) {
      collation = val;
      return this;
    }

    public Builder let(Document val) {
      let = val;
      return this;
    }

    public AggregateParams build() {
      return new AggregateParams(this);
    }
  }
}
