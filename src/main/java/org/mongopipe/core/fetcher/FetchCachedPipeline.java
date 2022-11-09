package org.mongopipe.core.fetcher;

import org.mongopipe.core.model.PipelineBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FetchCachedPipeline<T extends PipelineBase> implements FetchPipeline<T> {

    private final FetchPipeline<T> fetchPipelineStoreDelegate;
    Map<String, T> cache = new ConcurrentHashMap<>();

    public FetchCachedPipeline(FetchPipeline<T> fetchPipelineStoreDelegate) {
        this.fetchPipelineStoreDelegate = fetchPipelineStoreDelegate;
    }


    @Override
    public List<T> getAll() {
        return cache.entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public T getById(String id) {
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
