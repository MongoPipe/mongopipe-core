package org.mongopipe.core.fetcher;

import java.util.List;

public interface FetchPipeline<T> {

    List<T> getAll();

    T getById(String id);

    default void update() {

    }
}
