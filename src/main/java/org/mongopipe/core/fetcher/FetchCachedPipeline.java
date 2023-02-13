package org.mongopipe.core.fetcher;

import org.mongopipe.core.model.Pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FetchCachedPipeline implements FetchPipeline {

  private final FetchPipeline fetchPipelineStoreDelegate;
  // For in memory use a cache library or map implementation(but without collisions, unlike Java default Map implementations). By default
  // disable cache.
  Map<String, Pipeline> cache = new ConcurrentHashMap<>();

  public FetchCachedPipeline(FetchPipeline fetchPipelineStoreDelegate) {
    this.fetchPipelineStoreDelegate = fetchPipelineStoreDelegate;
  }


  @Override
  public List<Pipeline> getAll() {
    return cache.entrySet().stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public Pipeline getById(String id) {
    return cache.get(id);
  }

  @Override
  public void update() {
    cache.clear();
    synchronized (cache) {
      fetchPipelineStoreDelegate.getAll()
          .forEach(obj -> cache.put(obj.getId(), obj));
    }
  }

}
