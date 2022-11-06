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

package org.mongopipe.core.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import junit.framework.TestCase;
import org.mongopipe.core.Pipelines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Consider replacing with junit jupiter @Extension as @Before annotated method will not work if test class is extending TestCase.
 */
public abstract class AbstractMongoDBTest extends TestCase {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractMongoDBTest.class);

  /**
   * please store Starter or RuntimeConfig in a static final field
   * if you want to use artifact store caching (or else disable caching)
   */
  public static final MongodStarter STARTER = MongodStarter.getDefaultInstance();

  public MongodExecutable mongodExecutable;
  public MongodProcess mongod;

  public static int PORT;
  public static MongodConfig MONGOD_CONFIG;
  static {
    try {
      // These 2 needs to be static pe JVM or Class in order for the tests to not fail.
      // See https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/blob/de.flapdoodle.embed.mongo-3.5.0/README.md#usage---optimization
      PORT = Network.getFreeServerPort();
      MONGOD_CONFIG = MongodConfig.builder()
          .version(Version.V4_4_17)
          .net(new Net(PORT, Network.localhostIsIPv6()))
          .build();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public MongoDatabase db;
  public MongoClient mongoClient;

  protected void beforeEach() {  // See TO DO on class.
    newPipelinesConfig("pipelines_store", false);
  }

  @Override
  protected void setUp() throws Exception {
    LOG.info("---------- Database setup ----------");
    mongodExecutable = STARTER.prepare(MONGOD_CONFIG);
    mongod = mongodExecutable.start();

    ConnectionString connectionString = new ConnectionString("mongodb://localhost:" + PORT);
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .build();
    mongoClient = MongoClients.create(mongoClientSettings);
    db = mongoClient.getDatabase("test");

    super.setUp();
    beforeEach();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    db.drop();
    mongod.stop();
    mongodExecutable.stop();
  }

  protected void newPipelinesConfig(String storeCollection, boolean cacheEnabled) {
    // Consider this helper versus @Before because it allows configuration.
    Pipelines.newConfig()
        .uri("mongodb://localhost:" + PORT)
        .databaseName("test")
        .storeCollection(storeCollection)
        .storeCacheEnabled(cacheEnabled)
        .repositoriesScanPackage("org.mongopipe")
        .build();
  }
}