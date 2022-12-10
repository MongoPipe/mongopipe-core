/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mongopipe.core.annotation;

import org.bson.Document;
import org.mongopipe.core.config.MongoPipeConfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.mongopipe.core.runner.context.RunContextProvider.DEFAULT_CONTEXT_ID;

/**
 * Annotation used to mark an interface as containing pipeline run methods or store crud methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Store {
  /**
   * The {@link MongoPipeConfig#id} in case user has multiple databases or multiple PipelineStoreConfig
   * configurations.
   */
  String configurationId() default DEFAULT_CONTEXT_ID;

  /**
   * Specify the mappings of each item. Observe that the store decides where to put the items and not vice versa (i.e. item is STORAGE
   * AGNOSTIC). The hardcoded config can be also stored in db or in a file like a relational definition tree and referenced by id
   * or defaulted. Another minor reason is that same entity might be stored in different stores/collections.
   * @return
   * e.g. <code>
   *   @Store(
   *   items = {
   *    @Item(type=MyItem.class, collection="my_items_collection")
   *    ...
   *   })
   * </code>
   */
  Item[] items() default {};

  /**
   * Specify the item this Store handles by default for methods not using DB stored pipelines.
   * Alternatively: extend CrudStore<ItemClass, IdClass>
   */
  Class defaultItem() default Document.class;
}
