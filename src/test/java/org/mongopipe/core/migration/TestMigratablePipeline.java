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

package org.mongopipe.core.migration;

import org.mongopipe.core.migration.source.MigratablePipeline;
import org.mongopipe.core.model.Pipeline;

public class TestMigratablePipeline implements MigratablePipeline {
  private Long lastModifiedTime;
  private Pipeline pipeline;

  public TestMigratablePipeline(Long lastModifiedTime, Pipeline pipeline) {
    this.lastModifiedTime = lastModifiedTime;
    this.pipeline = pipeline;
  }

  private TestMigratablePipeline(Builder builder) {
    setLastModifiedTime(builder.lastModifiedTime);
    setPipeline(builder.pipeline);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public String getSourceName() {
    return null;
  }

  public static final class Builder {
    private Long lastModifiedTime;
    private Pipeline pipeline;

    private Builder() {
    }

    public Builder lastModifiedTime(Long val) {
      lastModifiedTime = val;
      return this;
    }

    public Builder pipeline(Pipeline val) {
      pipeline = val;
      return this;
    }

    public TestMigratablePipeline build() {
      return new TestMigratablePipeline(this);
    }
  }

  public void setLastModifiedTime(Long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public void setPipeline(Pipeline pipeline) {
    this.pipeline = pipeline;
  }
}