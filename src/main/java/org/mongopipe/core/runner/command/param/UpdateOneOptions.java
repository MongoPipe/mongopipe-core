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

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

import static org.mongopipe.core.runner.command.param.CommandOptions.TYPE_KEY;

/**
 * Stores parameters for <a href="https://www.mongodb.com/docs/manual/reference/method/db.collection.updateOne/">updateOne</a>.
 */
@BsonDiscriminator(value = UpdateOneOptions.TYPE, key = TYPE_KEY)
public class UpdateOneOptions extends BaseUpdateParams {
  public static final String TYPE = "updateOne";
  private final String type = TYPE;

  public UpdateOneOptions() {
  }

  @Override
  public String getType() {
    return type;
  }

  private UpdateOneOptions(Builder builder) {
    setFilter(builder.filter);
    setUpsert(builder.upsert);
    setHint(builder.hint);
    setWriteConcern(builder.writeConcern);
    setCollation(builder.collation);
    setArrayFilters(builder.arrayFilters);
    setBypassDocumentValidation(builder.bypassDocumentValidation);
    setComment(builder.comment);
    setLet(builder.let);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Document filter;
    private Boolean upsert;
    private String hint;
    private Document writeConcern;
    private Collation collation;
    private List<Document> arrayFilters;
    private Boolean bypassDocumentValidation;
    private String comment;
    private Document let;

    private Builder() {
    }

    public Builder filter(Document val) {
      filter = val;
      return this;
    }

    public Builder upsert(Boolean val) {
      upsert = val;
      return this;
    }

    public Builder hint(String val) {
      hint = val;
      return this;
    }

    public Builder writeConcern(Document val) {
      writeConcern = val;
      return this;
    }

    public Builder aggregateCollation(Collation val) {
      collation = val;
      return this;
    }

    public Builder arrayFilters(List<Document> val) {
      arrayFilters = val;
      return this;
    }

    public Builder bypassDocumentValidation(Boolean val) {
      bypassDocumentValidation = val;
      return this;
    }

    public Builder comment(String val) {
      comment = val;
      return this;
    }

    public Builder let(Document val) {
      let = val;
      return this;
    }

    public UpdateOneOptions build() {
      return new UpdateOneOptions(this);
    }
  }
}
