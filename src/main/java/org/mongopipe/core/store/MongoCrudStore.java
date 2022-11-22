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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.InsertOneResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.mongopipe.core.annotation.Item;
import org.mongopipe.core.annotation.Store;
import org.mongopipe.core.config.MongoPipeConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.model.Pipeline;
import org.mongopipe.core.runner.context.RunContext;
import org.mongopipe.core.util.BsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;

import static com.mongodb.client.model.Filters.eq;
import static org.mongopipe.core.runner.evaluation.BsonParameterEvaluator.PARAMETER_PATTERN;
import static org.mongopipe.core.util.ReflectionUtil.getFieldsAnnotatedWith;
import static org.mongopipe.core.util.ReflectionUtil.getMethodsAnnotatedWith;

public class MongoCrudStore<ItemType, Id> implements CrudStore<ItemType, Id> {

  private final RunContext runContext;
  private Class itemClass;
  private final String collectionName;
  private boolean dollarSignSensitive;

  public MongoCrudStore(RunContext runContext, Class storeClass) {
    this.runContext = runContext;
    //itemClass = getClassGenericType(storeClass); // TODO: fix for allowing interface extending CrudStore and thus no need for defaultItem.
    Store annotation = ((Store)storeClass.getAnnotation(Store.class));
    ItemInfo itemInfo = getStoreDefaultItem(annotation, storeClass);

    itemClass = itemInfo.getType();
    if (itemClass == Pipeline.class) {
      dollarSignSensitive = true; // Because replaceOne or findOneAndUpdate do not allow using $ prefixed fields inside bson.
    }
    collectionName = extractCollectionName(itemInfo.getCollection());
  }

  @Data
  @AllArgsConstructor
  private class ItemInfo {
    private Class type;
    private String collection;
    public ItemInfo(Item item) {
      setType(item.type());
      setCollection(item.collection());
    }
  }

  private ItemInfo getStoreDefaultItem(Store annotation, Class storeClass) {
    ItemInfo defaultItem = new ItemInfo(Document.class, null);
    defaultItem.setType(Document.class);

    if (annotation.defaultItem() == null) {
      // If missing default item consider @Store#items if only one found.
      if (annotation.items().length == 1) {
        defaultItem = new ItemInfo(annotation.items()[0]);
      }
    } else { // defaultItem specified
      if (annotation.defaultItem() == Document.class) { // This was the default annotation field value so ignore if there is only 1 item
        if (annotation.items().length == 1) {
          defaultItem = new ItemInfo(annotation.items()[0]);
        }
      } else {
        Optional<Item> itemOptional = Arrays.stream(annotation.items()).filter(item -> item.type() == annotation.defaultItem()).findFirst();
        if (!itemOptional.isPresent()) {
          // Will revisit to allow definition of all types on a single store.
          throw new MongoPipeConfigException("@Store#defaultItem can not be found in the list of items");
        }
        defaultItem = new ItemInfo(itemOptional.get());
      }
    }
    if (defaultItem.getCollection() == null) {
      throw new MongoPipeConfigException("For store '" + storeClass.getCanonicalName() + "' missing @Store#item collection name.");
    }
    return defaultItem;
  }

  private String extractCollectionName(String collection) {
    // With Spring will have to hook up Spring expression mechanism. TODO: refactor
    if (collection.startsWith("${mongoPipeConfig")) {
      Matcher matcher = PARAMETER_PATTERN.matcher(collection);
      List<String> groups = new ArrayList();
      while (matcher.find()) {
        groups.add(matcher.group());
      }
      try {
        if (groups.size() == 1) {
          String[] splits = groups.get(0).split("\\.");
          if (splits.length == 2 && splits[0].equals("mongoPipeConfig")) {
            Field field = MongoPipeConfig.class.getDeclaredField(splits[1]);
            field.setAccessible(true);
            collection = (String)field.get(runContext.getMongoPipeConfig());
          }
        }
      } catch (IllegalAccessException | NoSuchFieldException e) {
        throw new MongoPipeConfigException("Invalid expression:" + collection, e);
      }
    }
    return collection;
  }

  MongoCollection<ItemType> getCollection() {
    if (itemClass == null) {
      throw new MongoPipeConfigException("Missing store item type, e.g. @Store interface MyStore<ItemClass>");
    }
    return runContext.getMongoDatabase().getCollection(collectionName, itemClass);
  }

  private Id retrieveId(ItemType item) {
    // TODO: Refactor.
    try {
      List<Field> fields = getFieldsAnnotatedWith(itemClass, BsonId.class);
      if (fields.size() == 1) {
        fields.get(0).setAccessible(true);
        return (Id) fields.get(0).get(item);
      }
      List<Method> methods = getMethodsAnnotatedWith(itemClass, BsonId.class);
      if (methods.size() == 1 && methods.get(0).isAccessible()) {
        return (Id) methods.get(0).invoke(item);
      }

      fields = getFieldsAnnotatedWith(itemClass, BsonProperty.class);
      for (Field field : fields) {
        if ("_id".equals(field.getAnnotation(BsonProperty.class).value())) {
          fields.get(0).setAccessible(true);
          return (Id) fields.get(0).get(item);
        }
      }

      methods = getMethodsAnnotatedWith(itemClass, BsonProperty.class);
      for (Method method : methods) {
        if ("_id".equals(method.getAnnotation(BsonProperty.class).value()) && method.isAccessible()) {
          return (Id) method.invoke(item);
        }
      }

    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    throw new MongoPipeConfigException("Could not detect an ID field for the item class" + itemClass);
  }

  private ItemType insertOne(ItemType item) {
    InsertOneResult insertOneResult = getCollection().insertOne(item);
    return getCollection().find(eq("_id", insertOneResult.getInsertedId())).iterator().next();
  }

  @Override
  public ItemType save(ItemType item) {
    Id id = retrieveId(item);
    if (id == null) {
      return insertOne(item);
    }
    if (dollarSignSensitive) { // documents containing $ in the field names can use only insertOne.
      long matching = getCollection().countDocuments(eq("_id", id));
      if (matching == 0) {
        return insertOne(item);
      } else {
        getCollection().deleteOne(eq("_id", id));
        return insertOne(item);
      }
    } else {
      // NOTE: This is a full replace and not a partial update(patch). Probably for a patch update would have to revisit/add new method.
      FindOneAndReplaceOptions findOneAndReplaceOptions = new FindOneAndReplaceOptions();
      findOneAndReplaceOptions.upsert(true);
      findOneAndReplaceOptions.returnDocument(ReturnDocument.AFTER);
      //Bson document = BsonUtil.toBsonDocument(item);  // because findOneAndUpdate needs Bson unlike "insertOne"
      return getCollection().findOneAndReplace(eq("_id", retrieveId(item)), item, findOneAndReplaceOptions);
    }
  }

  @Override
  public Optional<ItemType> findById(Id id) {
    Iterator<ItemType> iterator = getCollection().find(eq("_id", BsonUtil.toBsonValue(id))).iterator();
    return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
  }

  @Override
  public Iterable<ItemType> findAll() {
    return getCollection().find();
  }

  @Override
  public long count() {
    return getCollection().countDocuments();
  }

  @Override
  public void deleteById(Id id) {
    getCollection().deleteOne(eq("_id", id));
  }

  @Override
  public void delete(ItemType item) {
    getCollection().deleteOne(eq("_id", retrieveId(item)));
  }

  @Override
  public void deleteAll() {
    getCollection().deleteMany(new Document());
  }

  @Override
  public boolean existsById(Id id) {
    return getCollection().countDocuments(eq("_id", id)) == 1;
  }
}
