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

package org.mongopipe.core.runner.invocation.handler;

import org.junit.Test;
import org.mongopipe.core.Stores;
import org.mongopipe.core.annotation.Item;
import org.mongopipe.core.annotation.Store;
import org.mongopipe.core.model.Pizza;
import org.mongopipe.core.util.AbstractMongoDBTest;

import static org.mongopipe.core.util.BsonUtil.loadResourceIntoDocumentList;

public class CrudInvocationHandlerTest extends AbstractMongoDBTest {

  @Store(
      items = {
          @Item(type = Pizza.class, collection = "pizzas")
      }
  )
  public interface MyCrudRestaurant {
    Pizza findById(Integer pizzaId);
  }

  @Test
  public void testMethodAutomaticDetectionAndCorrectInvocation() {
    db.getCollection("pizzas").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));

    Pizza pizza = Stores.get(MyCrudRestaurant.class)
        .findById(1);

    assertEquals("Pepperoni", pizza.getName());
  }


//  @Store(
//      // defaultItem = Pizza.class,  No need for default item as it is extending CrudStore.
//      items = {
//          @Item(type = Pizza.class, collection = "pizzas")
//      }
//  )
//  public interface MyCrudRestaurant2 extends CrudStore<Pizza, Integer> {
//
//  }
//  @Test
//  public void testMethodAutomaticDetectionAndCorrectInvocationWhenMethodIsInherited() {
//    db.getCollection("pizzas").insertMany(loadResourceIntoDocumentList("runner/pipelineRun/data.bson"));
//
//    Optional<Pizza> pizza = Stores.get(MyCrudRestaurant2.class)
//        .findById(1);
//
//    assertEquals("Pepperoni", pizza.get().getName());
//  }


}
