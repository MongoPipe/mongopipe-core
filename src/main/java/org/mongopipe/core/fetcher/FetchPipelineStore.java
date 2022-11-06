package org.mongopipe.core.fetcher;

import com.mongodb.client.MongoCollection;
import lombok.AllArgsConstructor;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.model.PipelineBase;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

@AllArgsConstructor
public class FetchPipelineStore<T extends PipelineBase> implements FetchPipeline<T> {

    private final PipelineRunConfig pipelineRunConfig;
    private final Class<T> classType;

    @Override
    public List<T> getAll() {
        return StreamSupport.stream(getCollection().find().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public T getById(String id) {
        return getCollection().find(eq("_id", id)).first();
    }

    private MongoCollection<T> getCollection() {
        return pipelineRunConfig.getMongoDatabase().getCollection(pipelineRunConfig.getStoreCollection(), classType);
    }


}
