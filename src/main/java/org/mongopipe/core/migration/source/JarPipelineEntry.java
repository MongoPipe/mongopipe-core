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

package org.mongopipe.core.migration.source;

public class JarPipelineEntry {
  private String path;
  private Long lastModifiedTime;
  private String jarPath;

  public JarPipelineEntry(String path, Long lastModifiedTime, String jarPath) {
    this.path = path;
    this.lastModifiedTime = lastModifiedTime;
    this.jarPath = jarPath;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(Long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public String getJarPath() {
    return jarPath;
  }

  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }

  @Override
  public java.lang.String toString() {
    return "JarPipelineEntry(path=" + this.getPath() + ", lastModifiedTime=" + this.getLastModifiedTime() + ", jarPath=" + this.getJarPath() + ")";
  }
}