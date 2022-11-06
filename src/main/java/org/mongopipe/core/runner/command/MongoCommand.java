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

package org.mongopipe.core.runner.command;

<<<<<<<< HEAD:src/main/java/org/mongopipe/core/runner/command/MongoCommand.java
public interface MongoCommand {

  Object run();
========
public enum PipelineCommandType {
  AGGREGATE, UPDATE_ONE, UPDATE_MANY, FIND_ONE_AND_UPDATE, FIND_AND_MODIFY;
>>>>>>>> main:src/main/java/org/mongopipe/core/model/PipelineCommandType.java
}
