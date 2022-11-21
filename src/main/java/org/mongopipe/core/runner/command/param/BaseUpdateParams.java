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

public abstract class BaseUpdateParams extends CommandOptions {
  protected Document filter;
  protected Boolean upsert;
  protected String hint;
  protected Document writeConcern;
  protected Collation collation;
  protected List<Document> arrayFilters;
  protected Boolean bypassDocumentValidation;
  protected String comment;
  protected Document let;

  public Document getFilter() {
    return filter;
  }

  public void setFilter(Document filter) {
    this.filter = filter;
  }

  public Boolean getUpsert() {
    return upsert;
  }

  public void setUpsert(Boolean upsert) {
    this.upsert = upsert;
  }

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public Document getWriteConcern() {
    return writeConcern;
  }

  public void setWriteConcern(Document writeConcern) {
    this.writeConcern = writeConcern;
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

  public Boolean getBypassDocumentValidation() {
    return bypassDocumentValidation;
  }

  public void setBypassDocumentValidation(Boolean bypassDocumentValidation) {
    this.bypassDocumentValidation = bypassDocumentValidation;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Document getLet() {
    return let;
  }

  public void setLet(Document let) {
    this.let = let;
  }
}
