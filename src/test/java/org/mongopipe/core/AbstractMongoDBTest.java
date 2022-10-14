/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
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

package org.mongopipe.core;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import junit.framework.TestCase;

public abstract class AbstractMongoDBTest extends TestCase {


  /**
   * please store Starter or RuntimeConfig in a static final field
   * if you want to use artifact store caching (or else disable caching)
   */
  private static final MongodStarter starter = MongodStarter.getDefaultInstance();

  private static MongodExecutable mongodExecutable;
  private static MongodProcess mongod;


  protected int port;

  @Override
  protected void setUp() throws Exception {
    // If running once consider adding shutdown hook and "if not null" test
    MongodStarter starter = MongodStarter.getDefaultInstance();

    port = Network.getFreeServerPort();
    MongodConfig mongodConfig = MongodConfig.builder()
        .version(Version.Main.PRODUCTION)
        .net(new Net(port, Network.localhostIsIPv6()))
        .build();


    mongodExecutable = starter.prepare(mongodConfig);
    mongod = mongodExecutable.start();

    super.setUp();

    //    ConnectionString connectionString = new ConnectionString("mongodb://localhost:" + port);
    //    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
    //        .applyConnectionString(connectionString)
    //        .build();
    //    MongoClient mongoClient = MongoClients.create(mongoClientSettings);
        //mongoClient.listDatabaseNames().first();
    //    db.createCollection("testCol");
    //
    //    db.getCollection("testCol").insertMany(Arrays.asList(Document.parse("{\"a\": 1}]"), Document.parse("{\"b\": 1}]")));
    //    Iterable iterable = db.getCollection("testCol").find(Document.parse("{}"));
    //    LOG.info("documents: {}", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    mongod.stop();
    mongodExecutable.stop();
  }
}