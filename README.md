# mongopipe-core

<a href="https://github.com/MongoPipe/">
<img src="https://github.com/MongoPipe/mongopipe-core/blob/main/docs/mongopipe.gif?raw=true" alt="logo.png" height="150px" align="right"/>
</a>

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/MongoPipe/mongopipe-core/blob/master/LICENSE)
[![Open Source](https://badges.frapsoft.com/os/v3/open-source.svg)](https://opensource.org/)
[![Supported JVM](https://img.shields.io/badge/supported%20JVM-8%2C%209+%20(19)-blueviolet)](https://img.shields.io/badge/supported%20JVM-8%2C%209+%20(19)-blueviolet)
[![Maven Central with version prefix filter](https://maven-badges.herokuapp.com/maven-central/org.mongopipe/mongopipe-core/badge.svg)](https://search.maven.org/artifact/org.mongopipe/mongopipe-core/1.0/jar)
[![GitHub open issues](https://img.shields.io/github/issues/mongopipe/mongopipe-core?color=GREEN)](https://img.shields.io/github/issues/mongopipe/mongopipe-core?color=GREEN)
[![GitHub open issues](https://img.shields.io/github/last-commit/mongopipe/mongopipe-core)](https://img.shields.io/github/last-commit/mongopipe/mongopipe-core)


# Intro
This is for MongoDB aggregation pipeline management. <br>
A MongoDB **aggregation pipeline is a BSON document, so store it in the database like other BSON documents.** <br>
**Hardcoding** is avoided. Code is static. Business rules are dynamic. <br>
Usage examples:
* You are doing fraud detection using pipelines, a DBA might like to tune **urgently** some pipelines rules according to a newly detected fraud risk.
* You have an UI and a client or administrator wants to change **easily** the values displayed by a dynamic combo box(pipeline backed), or to add new chart(pipeline backed) without waiting for a dedicated release with the new functionality.
* You have multiple reports backed by materialized views or pipelines. You want to easily change the reports via API.

MongoDB pipelines can be used for both **querying and updating** the data.<br>
No hardcoding meaning: <br> ![logo](docs/vs.png ) <br>

# Documentation
Documentation is hosted at [https://www.mongopipe.org/](https://www.mongopipe.org/). 

# 3 easy steps.
1. [Configuration](#1-configuration)
2. [Create your @Store](#2-create-your-store)
3. [Create BSON pipeline](#3-create-bson-pipeline)

## 1. Configuration

Maven dependency
```xml
<dependency>
  <groupId>org.mongopipe</groupId>
  <artifactId>mongopipe-core</artifactId>
  <version>X.Y.Z</version> <!-- Get latest from Maven Central or https://mvnrepository.com/artifact/org.mongopipe/mongopipe-core -->
</dependency>
```
```java
Stores.registerConfig(MongoPipeConfig.builder()
  .uri("<mongo uri>")
  .databaseName("<database name>")
  //.mongoClient(optionallyForCustomConnection)
  .build());
```
Find more examples in samples [repo](https://github.com/MongoPipe/Examples).

## 2. Create your store
```java
@Store
public interface MyRestaurant {
  @PipelineRun("matchingPizzas") // the db pipeline id, if missing is defaulted to method name. 
  Stream<Pizza> getMatchingPizzas(String pizzaSize);      
}    

// Running. 
// A. With Spring framework (needs "mongopipe-spring" dependency):
@Autowired
MyRestaurant myRestaurant;
...
myRestaurant.getMatchingPizzas("MEDIUM", ...);

// B. Without Spring:
Stores.from(MyRestaurant.class)
  .getMatchingPizzas("MEDIUM");

```
**NOTE**:
1. For **generic running** usages like the ones in the Intro section, meaning no need for pipeline stores(@Store annotated), you can use the
   `Pipelines.getRunner().run` method.  More here: [Generic creation and running](#dynamic-creation-and-running) <br>
   You only need the pipeline document to exist in the database collection (*pipeline_store*) or to be provided inline.
2. The parameters actual values provided are expected to be in the same order as in the pipeline template. For clearer identification
   annotate using `@Param` the method parameter and provide the template parameter name: <br>
   `List<Pizza> getMatchingPizzas(@Param("pizzaSize") String pizzaSize)`.
3. As secondary functionality, it supports generation of a CRUD operation just from the method naming similar with Spring Data.
   See [CRUD stores](#crud-stores)

## 3. Create BSON pipeline
Create resource file `myFirstPipeline.bson` that will be automatically inserted(via migration process) in the database collection
`pipeline_store`:
```bson
{
 "id": "matchingPizzas",
 "collection": "pizzas",
 "pipeline": [
   {
      $match: { size: "${pizzaSize}" }
   },
   { 
      $sort : { name : 1 } 
   }
 ]
}
```
For a list of all possible fields that you can use check the class 
[Pipeline](https://github.com/MongoPipe/mongopipe-core/blob/main/src/main/java/org/mongopipe/core/model/Pipeline.java) javadoc. <br>
Store the above bson file in your **source code**, under `src/main/resources/pipelines` (configurable in step 1 
[configuration](#1-configuration) via `MongoPipeConfig#migrationConfig#pipelinesPath`).<br>
On migration (at process startup time) all the pipelines from that folder will be created(if new) or updated(if changed) in the database collection
`pipeline_store`. Any future changes to the pipeline files will be detected and reflected in the database during migration run check. <br> 
If you are not using Spring and *mongopipe-spring* dependency you need to manually call the migration at process startup using `Pipelines.startMigration()`.<br>

Pipeline store collection:
![db store](/docs/pipeline_store.png ) <br>

NOTE:
1. The pipelines can be also **manually** created using the [PipelineStore API](#dynamic-creation-and-running).   
2. The file above although static it is input into the migration utility at process startup and thus seeded in the database. It can then be
   updated at runtime via the PipelineStore API or the file can be manually modified and on process startup it will be
   automatically updated in the database by the migration process. More on [Migration](#migration).
3. **The parameters form is `"${paramName}"`**. <br>
   Parameters inside the pipeline template **must** be strings (e.g. `"..": "${paramName}"`) in order to be a valid BSON.
   On pipeline run the **actual parameters values can be of any type including complex types: lists, maps, pojos** as long as it can be
   converted to a BSON type.<br>
   For example on pipeline running if the actual parameter value is an integer (e.g. 10) the string value: <br>
   `"x": "${paramName}",` will become an integer value:<br>
   `"x": 10,`

# Dynamic creation and running
Sometimes instead of using an interface to define the pipeline run methods you can instead manually both create and run a pipeline:
```java
    // Use PipelineStore for any CRUD operations on pipelines. 
    PipelineStore pipelineStore = Pipelines.getStore();

    // 1. From a String:
    String bsonStringPipeline = "{ \"id\": \"matchingPizzas\", \"collection\": \"pizzas\", \"pipeline\": [ ...";
    Pipeline pipeline = BsonUtil.toPojo(bsonString, Pipeline.class);
    pipelineStore.create(pipeline);
    
    // 2. Dynamically using BSON API, static imports are from Mongo driver API class: com.mongodb.client.model.Aggregates / Filters.        
    Bson matchStage = match(and(eq("size", "${size}"), eq("available", "${available}")));
    Bson sortByName = sort(descending("name"));
    pipelineStore.create(Pipeline.builder()        
        .id("matchingPizzas")
        .pipeline(asList(matchStage, sortByName))
        //.pipelineAsString("...") can be also provided as a string
        .collection("pizzas")
        .build());
    
    // Run
    PipelineRunner pipelineRunner = Pipelines.getRunner();
    List<Pizza> pizzas = pipelineRunner.run("matchingPizzas", Pizza.class, Maps.of("size", "medium", "available", true)) // Returns a stream
        .collect(Collectors.toList());
```
NOTE:
1. Store obtained via `Pipelines.getStore()` can be used also to create, update and delete pipelines.
2. You can also parameterize an entire pipeline stage/subparts of a stage and send an object, array, pojo or bson type as an actual parameter. For example
   when sorting on multiple fields the `{field1: 1, field2: -1}` can be provided as a Java map or pojo class

# Migration
The migration will be started automatically on process start if using Spring framework (*mongopipe-spring* dependency required) or manually
by invoking:
`Pipelines.startMigration()`. <br>
It will detect all the pipelines that have changed or are new by comparing checksums of the source pipelines with the existing pipelines in
the database. Then it will take appropriate action (create or update) on each pipeline. <br>
The prior value of an updated pipeline will be saved in the `pipeline_store_history` collection for backup purposes. This is configurable.

# CRUD stores
A @Store annotated interface can support both @PipelineRun methods and also CRUD methods by naming convention.<br>
The method signature must match one of the methods from `org.mongopipe.core.store.CrudStore`. E.g.:
```java
@Store(
    items = {
        @Item(type = Pizza.class, collection = "pizzas")
    }
)
public interface PizzaRestaurant {
  Pizza save(Pizza pizza);
  Pizza findById(String id);
  Iterable<Pizza> findAll();
  Long count();
}
```
NOTE:
1. The store(via the @Store annotation) decides where to put the items and not vice versa meaning an item type class is storage
   agnostic. Thus, the annotation `@Store#items` field acts as a database mapping definition.
2. This feature is secondary, main feature is to manage and run pipelines.

# More examples
Find more examples in samples [repo](https://github.com/MongoPipe/Examples).

# Update operations
For performing data updates: 
1. Without pipelines, you can use [CRUD stores](#crud-stores)
2. With pipelines, using [update stages](https://www.mongodb.com/docs/manual/tutorial/update-documents-with-aggregation-pipeline/) like for example the `$replaceRoot`.
3. With pipelines, using dedicated commands like for example [findOneAndUpdate](findOneAndUpdate()) which can be run by setting `Pipeline#commandOptions`.
   The findOneAndUpdate allows also to insert the document if it does not exist.

# TODO
- Use JavaDoc links in the documentation instead of class names only.

# Support and get in touch
<img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/settings-outline.svg" width="20"/><img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/bug-outline.svg" width="20"/>
If you have a bug or an idea, browse the open issues and create a new one. <br>

<img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/mail-outline.svg" width="20"/> We like direct discussions. Check email address on the github profile of the committers.


