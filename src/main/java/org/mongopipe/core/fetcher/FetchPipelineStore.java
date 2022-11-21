package org.mongopipe.core.fetcher;

import lombok.AllArgsConstructor;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.store.PipelineCrudStore;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AllArgsConstructor
public class FetchPipelineStore implements FetchPipeline {

  private final PipelineCrudStore crudStore;

  @Override
  public List<Pipeline> getAll() {
    return StreamSupport.stream(crudStore.findAll().spliterator(), false)
        .collect(Collectors.toList());
  }

  @Override
  public Pipeline getById(String id) {
    return crudStore.findById(id).orElse(null);
  }


}
