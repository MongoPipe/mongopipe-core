package org.mongopipe.core.model;

import org.bson.codecs.pojo.annotations.BsonId;

public abstract class MongoEntity {
  /**
   * Uniquely identifies the stored document. For example it is used in the @PipelineRun annotation.   *
   */
  @BsonId
  protected String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
