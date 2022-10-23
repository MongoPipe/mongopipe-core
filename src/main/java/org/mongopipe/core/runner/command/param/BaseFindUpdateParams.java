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

import java.util.List;

public abstract class BaseFindUpdateParams implements CommandAndParams {
  private Document filter;
  private Document projection;
  private Document sort;
  private Long maxTimeMS;
  private Boolean upsert;
  private String returnDocument;
  private Boolean returnNewDocument;
  private Collation collation;
  private List<Document> arrayFilters;

  public Document getFilter() {
    return filter;
  }

  public void setFilter(Document filter) {
    this.filter = filter;
  }

  public Document getProjection() {
    return projection;
  }

  public void setProjection(Document projection) {
    this.projection = projection;
  }

  public Document getSort() {
    return sort;
  }

  public void setSort(Document sort) {
    this.sort = sort;
  }

  public Long getMaxTimeMS() {
    return maxTimeMS;
  }

  public void setMaxTimeMS(Long maxTimeMS) {
    this.maxTimeMS = maxTimeMS;
  }

  public Boolean getUpsert() {
    return upsert;
  }

  public void setUpsert(Boolean upsert) {
    this.upsert = upsert;
  }

  public String getReturnDocument() {
    return returnDocument;
  }

  public void setReturnDocument(String returnDocument) {
    this.returnDocument = returnDocument;
  }

  public Boolean getReturnNewDocument() {
    return returnNewDocument;
  }

  public void setReturnNewDocument(Boolean returnNewDocument) {
    this.returnNewDocument = returnNewDocument;
  }

  public Collation getCollation() {
    return collation;
  }

  public void setCollation(Collation collation) {
    this.collation = collation;
  }

  public List<Document> getArrayFilters() {
    return arrayFilters;
  }

  public void setArrayFilters(List<Document> arrayFilters) {
    this.arrayFilters = arrayFilters;
  }
}
