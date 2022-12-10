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

import java.util.Optional;

/**
 * Your @Store will automatically support a CRUD method if it matches the signature in the interface.
 * A generic @Store can keep objects of different types. But the methods listed here are the ones supported for every item type.
 *
 *
 *
 * @param <Item>
 * @param <Id>
 */
public interface CrudStore<Item, Id> {

  /**
   * Creates or updates pojo in database and returns the updated document.
   */
  Item save(Item item);

  Optional<Item> findById(Id id);

  Iterable<Item> findAll();

  long count();

  void deleteById(Id id);

  void delete(Item item);

  void deleteAll();

  boolean existsById(Id id);

}
