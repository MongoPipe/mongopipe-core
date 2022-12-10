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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CustomLog
public class FileMigratablePipelineScanner implements MigratablePipelineScanner {
  public static final List<String> CANDIDATE_EXTENSIONS = Arrays.asList(new String[] { "bson", "json" });

  private List<File> findPipelinesInFolder(String directoryName) {
    File directory = new File(directoryName);
    List<File> resultList = new ArrayList<>();

    File[] fList = directory.listFiles();
    for (File file : fList) {
      if (file.canRead()) {
        if (file.isHidden()) {
          LOG.debug("Hidden file ignored:" + file.getAbsolutePath());
        }
        if (file.isFile() && CANDIDATE_EXTENSIONS.contains(getExtension(file.getName()))) {
          resultList.add(file);
        } else if (file.isDirectory()) {
          resultList.addAll(findPipelinesInFolder(file.getAbsolutePath()));
        }
      }
    }
    return resultList;
  }

  public static String getExtension(String filename) {
    return filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";
  }

  @Override
  public List<MigratablePipeline> loadPipelinesFromLocation(URL url, MigrationConfig migrationConfig) {
    if ("file".equalsIgnoreCase(url.getProtocol())) {
      LOG.debug("Loading pipeline from file {}", url.getPath());
      try {
        return findPipelinesInFolder(Paths.get(url.toURI()).toFile().getAbsolutePath()).stream()
            .map(file -> new FileMigratablePipeline(file))
            .collect(Collectors.toList());
      } catch (URISyntaxException e) {
        throw new MongoPipeMigrationException(e.getMessage(), e);
      }
    } else {
      return Collections.emptyList();
    }
  }
}
