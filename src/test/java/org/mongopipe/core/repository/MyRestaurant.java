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

package org.mongopipe.core.repository;

import org.bson.Document;
import org.mongopipe.core.annotation.Param;
import org.mongopipe.core.annotation.PipelineRun;
import org.mongopipe.core.annotation.PipelineRepository;
import org.mongopipe.core.runner.Pizza;

import java.util.List;

@PipelineRepository  // Optional, TODO: unit test.
public interface MyRestaurant {

  @PipelineRun("pipelineOne")
  List<Document> runMyFirstPipeline(@Param("pizzaSize") String pizzaSize);

  @PipelineRun("matchingPizzasBySize")
  List<Pizza> getMatchingPizzas(@Param("pizzaSize") String pizzaSize);

  @PipelineRun("matchingPizzasByPrice")
  List<Pizza> getMatchingPizzasByPrice(@Param("pizzaPrice") Double price);

  @PipelineRun("updateOneMatchingPizza")
  Long updateOnePizzaByPizzaPrice(@Param("pizzaPrice") Integer pizzaPrice);

  @PipelineRun("findOnePizzaAndUpdate")
  Document findOneAndUpdate(@Param("pizzaPrice") Integer pizzaPrice);


}