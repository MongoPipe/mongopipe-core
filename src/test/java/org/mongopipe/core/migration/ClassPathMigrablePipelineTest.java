package org.mongopipe.core.migration;

import lombok.SneakyThrows;
import org.junit.Test;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.util.BsonUtil;
import org.mongopipe.core.util.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassPathMigrablePipelineTest {

    @Test
    @SneakyThrows
    public void checkGetLastModified() {

        //GIVEN
        ClasspathMigrablePipeline classpathMigrablePipeline = new ClasspathMigrablePipeline("runner/pipelineRun/data.bson");

        //WHEN
        long millis = classpathMigrablePipeline.getLastModifiedTime();

        //THEN
        assertTrue(millis > 0);
    }

    @Test
    @SneakyThrows
    public void checkGetPipeline() {
        //GIVEN
        String resourcePath = "runner/pipelineRun/matchingPizzasBySize.pipeline.bson";
        ClasspathMigrablePipeline classpathMigrablePipeline = new ClasspathMigrablePipeline(resourcePath);

        //WHEN
        Pipeline pipeline = classpathMigrablePipeline.getPipeline();

        //THEN
        String expectedPipelineAsString = TestUtil.getClasspathFileContent(resourcePath);

        assertEquals(BsonUtil.toPojo(expectedPipelineAsString, Pipeline.class).getPipeline(), pipeline.getPipeline());
    }
}
