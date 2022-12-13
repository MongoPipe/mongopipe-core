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

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongopipe.core.Stores;
import org.mongopipe.core.annotation.Item;
import org.mongopipe.core.annotation.Store;
import org.mongopipe.core.model.Bike;
import org.mongopipe.core.util.AbstractMongoDBTest;

public class CustomStoreTest extends AbstractMongoDBTest {

  @Store(items = {@Item(type = Bike.class, collection = "test")})
  public interface BikeStore {
    Bike save(Bike bike);

    Bike findById(ObjectId id);

    Long count();
  }

  @Test
  public void testSaveAndFind() {
    // Given
    String description = "some bike";
    Bike bike = new Bike();
    bike.setDescription(description);
    BikeStore bikeStore = Stores.from(BikeStore.class);

    // When
    Bike savedBike = bikeStore.save(bike);

    // Then
    assertNotNull(savedBike.getId());
    assertEquals(description, savedBike.getDescription());
    assertEquals(description, bikeStore.findById(savedBike.getId()).getDescription());
  }

  public static class Car {
    @BsonId private Long nonObjectId;
    private String description;

    public Long getNonObjectId() {
      return nonObjectId;
    }

    public void setNonObjectId(Long nonObjectId) {
      this.nonObjectId = nonObjectId;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  @Store(items = {@Item(type = Car.class, collection = "test")})
  public interface CarStore {
    Car save(Car car);

    Car findById(Long id);
  }

  @Test
  public void testSaveObjectWithIdBeingNonObjectId() {
    // Given
    String description = "some car";
    Car car = new Car();
    car.setNonObjectId(1L); // This is needed or else: "Invalid numeric type, found:
    // OBJECT_ID".
    car.setDescription(description);
    CarStore carStore = Stores.from(CarStore.class);

    // When
    Car savedCar = carStore.save(car);

    // Then
    assertNotNull(savedCar.getNonObjectId());
    assertEquals(description, carStore.findById(savedCar.getNonObjectId()).getDescription());
  }
}
