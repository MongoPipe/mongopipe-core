package org.mongopipe.core.fetcher;

import org.mongopipe.core.model.Pipeline;

import java.util.List;

public interface FetchPipeline {

  List<Pipeline> getAll();

  Pipeline getById(String id);

  default void update() {

  }
}
