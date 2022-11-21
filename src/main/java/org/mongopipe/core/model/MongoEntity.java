package org.mongopipe.core.model;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonId;

@Data
public abstract class MongoEntity {
  /**
   * Uniquely identifies the stored document. For example it is used in the @PipelineRun annotation.   *
   */
  @BsonId
  protected String id;
}
