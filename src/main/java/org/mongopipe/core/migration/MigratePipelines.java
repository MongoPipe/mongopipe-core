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

package org.mongopipe.core.migration;

/**
 *
 * TODO: If a pipeline is updated via PipelineStore then version is increased.
 * Since on org.mongopipe.core.migration the two will be different the one updated already existing in DB will take precendence.
 *
 * Simpler algorithm would be to compare version numbers but it does not compare actual pipeline content which may be modified directy in DB.
 * Thus it is necessary to compare contents or contents checksum for faster org.mongopipe.core.migration.
 */
public class MigratePipelines {
}
