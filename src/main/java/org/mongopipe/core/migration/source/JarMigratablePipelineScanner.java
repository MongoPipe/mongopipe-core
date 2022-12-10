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

import lombok.CustomLog;
import org.mongopipe.core.config.MigrationConfig;
import org.mongopipe.core.exception.MongoPipeMigrationException;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static org.mongopipe.core.migration.source.FileMigratablePipelineScanner.CANDIDATE_EXTENSIONS;
import static org.mongopipe.core.migration.source.FileMigratablePipelineScanner.getExtension;

@CustomLog
public class JarMigratablePipelineScanner implements MigratablePipelineScanner {

  private JarFile openJarFile(String separator, URL urlInsideJar) throws IOException {
    URLConnection urlConnection = urlInsideJar.openConnection();
    if (urlConnection instanceof JarURLConnection) {
      JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
      jarURLConnection.setUseCaches(false); // To avoid keeping resources, as will anyway close the jar.
      return jarURLConnection.getJarFile();
    } else {
      String jarDiscPath = urlInsideJar.getPath().replaceAll("file:", "");
      jarDiscPath = jarDiscPath.substring(0, jarDiscPath.indexOf(separator));
      return new JarFile(jarDiscPath);
    }
  }

  private List<JarPipelineEntry> getPipelineEntries(JarFile jarFile, String pipelinesPath, URL url) {
    String prefix = jarFile.getName().toLowerCase().endsWith(".war") ? "WEB-INF/classes/" : "";
    return jarFile.stream()
        .filter(entry -> entry.getName().startsWith(prefix + pipelinesPath) && CANDIDATE_EXTENSIONS.contains(getExtension(entry.getName())))
        .map(entry -> new JarPipelineEntry(entry.getName().substring(prefix.length()), entry.getTime(), url.getPath()))
        .collect(Collectors.toList());
  }

  @Override
  public List<MigratablePipeline> loadPipelinesFromLocation(URL url, MigrationConfig migrationConfig) {
    String protocol = url.getProtocol();
    if ("jar".equalsIgnoreCase(protocol) || "war".equalsIgnoreCase(protocol)) {
      String startPathSeparator = "war".equalsIgnoreCase(protocol) ? "*/" : "!/";
      try (JarFile jarFile = openJarFile(startPathSeparator, url)) {
        List<JarPipelineEntry> pipelineEntries = getPipelineEntries(jarFile, migrationConfig.getPipelinesPath(), url);

        return pipelineEntries.stream()
            .map(pipelineEntry -> new JarMigratablePipeline(pipelineEntry))
            .collect(Collectors.toList());
      } catch (IOException e) {
        throw new MongoPipeMigrationException(e);
      }
    } else {
      return Collections.emptyList();
    }
  }
}
