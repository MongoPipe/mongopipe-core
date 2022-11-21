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

package org.mongopipe.core.store;

import org.mongopipe.core.annotation.Item;
import org.mongopipe.core.annotation.Store;
import org.mongopipe.core.migration.model.Status;

import java.util.Optional;

@Store(
    items = {
        @Item(type = Status.class, collection = "${mongoPipeConfig.statusCollection}")
    }
)
public interface StatusStore {
  Optional<Status> findById(Long id);
  Status save(Status status);
}
