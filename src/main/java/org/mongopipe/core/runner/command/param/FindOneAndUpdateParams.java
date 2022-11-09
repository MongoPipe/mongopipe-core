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

import static org.mongopipe.core.runner.command.param.CommandAndParams.TYPE_KEY;

/**
 * Stores parameters for <a href="https://www.mongodb.com/docs/manual/reference/method/db.collection.findOneAndUpdate/">findOneAndUpdate</a>.
 */
@BsonDiscriminator(value = FindOneAndUpdateParams.TYPE, key = TYPE_KEY)
public class FindOneAndUpdateParams extends BaseFindUpdateParams {
  public static final String TYPE = "findOneAndUpdate";
  private final String type = TYPE;

  public FindOneAndUpdateParams() {
  }

  private FindOneAndUpdateParams(Builder builder) {
    setFilter(builder.filter);
    setProjection(builder.projection);
    setSort(builder.sort);
    setMaxTimeMS(builder.maxTimeMS);
    setUpsert(builder.upsert);
    setReturnDocument(builder.returnDocument);
    setReturnNewDocument(builder.returnNewDocument);
    setCollation(builder.collation);
    setArrayFilters(builder.arrayFilters);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getType() {
    return type;
  }

  public static final class Builder {
    private Document filter;
    private Document projection;
    private Document sort;
    private Long maxTimeMS;
    private Boolean upsert;
    private String returnDocument;
    private Boolean returnNewDocument;
    private Collation collation;
    private List<Document> arrayFilters;

    private Builder() {
    }

    public Builder filter(Document val) {
      filter = val;
      return this;
    }

    public Builder projection(Document val) {
      projection = val;
      return this;
    }

    public Builder sort(Document val) {
      sort = val;
      return this;
    }

    public Builder maxTimeMS(Long val) {
      maxTimeMS = val;
      return this;
    }

    public Builder upsert(Boolean val) {
      upsert = val;
      return this;
    }

    public Builder returnDocument(String val) {
      returnDocument = val;
      return this;
    }

    public Builder returnNewDocument(Boolean val) {
      returnNewDocument = val;
      return this;
    }

    public Builder collation(Collation val) {
      collation = val;
      return this;
    }

    public Builder arrayFilters(List<Document> val) {
      arrayFilters = val;
      return this;
    }

    public FindOneAndUpdateParams build() {
      return new FindOneAndUpdateParams(this);
    }
  }
}