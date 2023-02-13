# mongopipe-core

<a href="https://github.com/MongoPipe/">
<img src="https://github.com/MongoPipe/mongopipe-core/blob/main/docs/mongopipe.gif?raw=true" alt="logo.png" height="150px" align="right"/>
</a>

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/MongoPipe/mongopipe-core/blob/master/LICENSE)
[![Open Source](https://badges.frapsoft.com/os/v3/open-source.svg)](https://opensource.org/)
[![Supported JVM](https://img.shields.io/badge/supported%20JVM-8%2C%209+%20(19)-blueviolet)](https://img.shields.io/badge/supported%20JVM-8%2C%209+%20(19)-blueviolet)
[![Maven Central with version prefix filter](https://maven-badges.herokuapp.com/maven-central/org.mongopipe/mongopipe-core/badge.svg)](https://search.maven.org/artifact/org.mongopipe/mongopipe-core)
[![GitHub open issues](https://img.shields.io/github/issues/mongopipe/mongopipe-core?color=GREEN)](https://img.shields.io/github/issues/mongopipe/mongopipe-core?color=GREEN)
[![GitHub open issues](https://img.shields.io/github/last-commit/mongopipe/mongopipe-core)](https://img.shields.io/github/last-commit/mongopipe/mongopipe-core)
[![javadoc](https://javadoc.io/badge2/org.mongopipe/mongopipe-core/javadoc.svg)](https://javadoc.io/doc/org.mongopipe/mongopipe-core)

# Intro
How to store MongoDB aggregation pipelines in the database and run them?. <br>
A MongoDB **aggregation pipeline is a JSON document, so store it in the database.** <br>

<img src="https://github.com/MongoPipe/mongopipe-core/blob/main/docs/question.png height="200px" align="left"/>
* Runtime configurability
* Hardcoding avoided (code page long native queries)
* Pipelines are documents thus a structured format. 
* Avoid native queries with 'DB agnostic' libraries.

<p/>
Some use cases:
* **Runtime changes**. Want to be able to change underlying queries/rules and avoid the use of an additional abstraction layer(e.g. query builder) that might limit the full potential of the database.<br>
    E.g.: Configuring DB alerts/rules for a risk detection solution.
* **Urgent** changes by DBAs/admins are needed without waiting for patches/releases containing changed queries. <br>
   Thus the system can adapt quickly and the admin/dba doing them would not need coding or devops skills.
* **UI widgets data source customization**. An admin can easily customize a chart, a report or a widget data source by just editing the backing pipeline via a REST api.
   <br>For example a combo box display a limit of 5 countries stored in the DB, may need to display 10 countries filtered on different conditions
  based on the client(multitenant) id. Keeping a hardcoded query for each client would be too complex.  

# Getting started in 3 easy steps
1. [Configuration](#1-configuration)
2. [Create JSON pipelines](#2-create-json-pipelines)
3. [Run pipelines using @Stores](#3-run-pipelines)


## 1. Configuration

### With Spring:
Maven dependency:
```xml
<dependency>
    <groupId>org.mongopipe</groupId>
    <artifactId>mongopipe-spring</artifactId>
    <version>X.Y.Z</version> <!-- Get latest from Maven Central or https://mvnrepository.com/artifact/org.mongopipe/mongopipe-spring -->
</dependency>
```
Add 2 beans required for configuration and autostarting.
```java
@Bean
public MongoPipeConfig getMongoPipeConfig() {
  return MongoPipeConfig.builder()
      .uri("mongodb://...")
      .databaseName("database name")
      //.mongoClient(optionallyForCustomConnection) // e.g. for TLS
      .build();
}

@Bean
public MongoPipeStarter getMongoPipeStarter(MongoPipeConfig mongoPipeConfig) {
  return new MongoPipeStarter(mongoPipeConfig);
}
```
### Without Spring:
```xml
<dependency>
  <groupId>org.mongopipe</groupId>
  <artifactId>mongopipe-core</artifactId>
  <version>X.Y.Z</version> <!-- Get latest from Maven Central or https://mvnrepository.com/artifact/org.mongopipe/mongopipe-core -->
</dependency>
```
Manually register config.
```java
Stores.registerConfig(MongoPipeConfig.builder()
  .uri("<mongo uri>")
  .databaseName("<database name>")
  //.mongoClient(optionallyForCustomConnection)
  .build());
```

## 2. Create JSON pipelines
Create a JSON/BSON resource file `myFirstPipeline.json` that will be automatically **migrated**(inserted) in the database collection `pipeline_store` at startup.<br>
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
* Store the above json file in your **source code**, under `src/main/resources/pipelines` (configurable via `MongoPipeConfig#migrationConfig#pipelinesPath`).<br>
* On [Migration](#migration) (at process startup time) all the pipelines from that folder will be created(if new) or updated(if changed) 
  in the database collection `pipeline_store`. Any future changes to the pipeline files will be detected and reflected in the database during startup migration run check. <br>
  If you are not using Spring and *mongopipe-spring* dependency you need to manually call the migration on every process start using `Pipelines.startMigration()`.<br>
* For a list of all possible fields that you can use check the model class
  [Pipeline](https://github.com/MongoPipe/mongopipe-core/blob/main/src/main/java/org/mongopipe/core/model/Pipeline.java) javadoc. <br>


## 3. Run pipelines
This will bind code (interface methods) to pipelines stored in DB. Create just an interface:
```java
@Store
public interface MyRestaurant {
  @PipelineRun("matchingPizzas") // the db pipeline id, if missing is defaulted to method name. 
  Stream<Pizza> getMatchingPizzas(String pizzaSize);      
}    

// With Spring ("mongopipe-spring" dependency):
@Autowired
MyRestaurant myRestaurant;
...
myRestaurant.getMatchingPizzas("MEDIUM");

// Without Spring:
Stores.from(MyRestaurant.class)
  .getMatchingPizzas("MEDIUM");

```
**NOTE**:
1. The `@Store` class annotation is mandatory. The `@PipelineRun` annotation is optional and if missing will be defaulted to the method name. 
2. For **generic running** usages like the ones in the Intro section, meaning **no** need for pipeline stores(`@Store` annotated), you can use the
   `Pipelines.getRunner().run` method.  More here: [Generic creation and running](#dynamic-creation-and-running-with-criterias)
   **You can use this behind a REST api to generically create and run pipelines**. <br>
   You only need the pipeline document to exist in the database collection (*pipeline_store*) or to be provided at runtime.
3. The parameters actual values provided are expected to be in the same order as in the pipeline template. For clearer identification
   annotate using `@Param` the method parameter and provide the template parameter name: <br>
   `List<Pizza> getMatchingPizzas(@Param("pizzaSize") String pizzaSize)`.
4. As secondary functionality, it supports generation of a CRUD operation just from the method naming similar with Spring Data.
   See [CRUD stores](#crud-stores)
    

# More on pipeline files
* Pipeline files/sources end up (via migration) in the `pipeline_store` collection: ![db store](/docs/pipeline_store.png )
* Once migrated in the database pipelines can be independently updated/created at runtime using the [PipelineStore](https://javadoc.io/static/org.mongopipe/mongopipe-core/1.0/index.html?org/mongopipe/core/store/PipelineStore.html) CRUD API.<br>
  Future file updates to the pipeline file will be promoted to DB via automatic migration on startup.
* The pipelines can be also **manually** created/updated using the [dynamic way](#dynamic-creation-and-running-with-criterias).
* The json pipeline file although static is the input into the migration utility at process startup and thus seeded in the database. It can then be
   updated at runtime via the PipelineStore API or the seed file can be manually modified and on next process startup it will be
   automatically updated in the database by the migration process. More on [Migration](#migration).
* **The parameters form is `"${paramName}"`**. <br>
   Parameters inside the pipeline template **must** be strings (e.g. `"..": "${paramName}"`) in order to be valid JSON.
   On pipeline run the **actual parameters values can be of any type including complex types: lists, maps, pojos** as long as it can be
   converted to a JSON/BSON type.<br>
   For example on pipeline running if the actual parameter value is an integer (e.g. 10) the string value: <br>
   `"x": "${paramName}",` will become an integer value:<br>
   `"x": 10,`
* The pipeline files can have .json or .bson file extension. It is recommended to use .bson extension when the pipeline contains BSON types that are not standard JSON.


# Dynamic creation and running with criterias
Sometimes instead of using an interface to define the pipeline run methods you can instead manually both create and run a pipeline.
This is useful **if you do not want to create `@PipelineRun` annotated methods** for every pipeline run. By using `PipelineRunner` you can have a single endpoint to run any pipeline. <br>
Also **subparts** of the pipeline can be constructed in multiple ways.

```java
    // Use PipelineStore for any CRUD operations on pipelines. 
    PipelineStore pipelineStore = Pipelines.getStore();
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // You can create a pipeline dynamically or from criterias in several ways:

    // ********
    // 1. Using "Mongo BSON criterias API", static imports are from Mongo driver API class: com.mongodb.client.model.Aggregates/Filters/Sorts.
    // ********
    // Pipeline stages one by one.
    List<Bson> pipelineBson = Arrays.asList(
        // Static imports from com.mongodb.client.model.Aggregates/Filters/Sorts
        match(and(eq("size", "${size}"), eq("available", "${available}"))),
        sort(descending("price")),
        limit(3)
    );
    Pipeline dynamicPipeline = Pipeline.builder()
        .id("dynamicPipeline")
        .pipeline(pipelineBson)
        .collection("testCollection")
        .build();
    pipelineStore.create(dynamicPipeline);
    
    List<Pizza> pizzas = pipelineRunner.run("matchingPizzas", Pizza.class, Map.of("size", "medium", "available", true)) // Returns a stream
        .collect(Collectors.toList());

    
    // ********
    // 2. Replacing entire sections of a minimal static pipeline by making them variables.
    //    As parameter value provide Java types corresponding to JSON (e.g. maps, arrays) or POJO classes with getters and setters. 
    // ********
    
    // This will normally come from an outside source, provided by an admin/DBA.
    String jsonString = "{ \"_id\": \"dynamicPipeline\", \"version\": 1, \"collection\": \"testCollection\", \"pipelineAsString\":\"[" +
        "{\\\"$match\\\": {\\\"$and\\\": [{\\\"size\\\": \\\"${size}\\\"}, {\\\"available\\\": \\\"${available}\\\"}]}}," +
        "{\\\"$sort\\\": \\\"${sortMap}\\\" }," +
        "{\\\"$limit\\\": 3}]\" }";
    Pipeline pipeline = BsonUtil.toPojo(jsonString, Pipeline.class);
    pipelineStore.create(pipeline);

    // 3rd param is a map/object by itself and can be arbitrarily deep. Also an entire stage can be provided.
    // NOTE: Here we use a Map type as parameter value for 'sortMap' parameter. But any POJO class of your choice can be used
    // and needs just getters and setters in order to be convertible to JSON (BsonUtil#toBsonValue beeing called). 
    List<Pizza> pizzas = pipelineRunner.runAndList(pipeline.getId(), Pizza.class, Maps.of("size", "medium", "available", true, 
        "sortMap", Maps.of("price", -1, "name", 1)));

    
    // ********
    // 3. From plain JSON/BSON String. Thus entire pipeline can be provided by an admin/DBA.
    // ********
    String pipelineStages = BsonUtil.escapeJsonFieldValue("[" +
        "{\"$match\": {\"$and\": [{\"size\": \"${size}\"}, {\"available\": \"${available}\"}]}}," +
        "{\"$sort\": {\"price\": -1}}," +
        "{\"$limit\": 3}" +
        "]");
    String jsonString = "{ \"_id\": \"dynamicPipeline\", \"version\": 1, \"collection\": \"testCollection\"," +
        " \"pipelineAsString\":\"" + pipelineStages + "\" }";
    Pipeline pipeline = BsonUtil.toPojo(jsonString, Pipeline.class);
    pipelineStore.create(pipeline);
    List<Pizza> pizzas = pipelineRunner.run("matchingPizzas", Pizza.class, Map.of("size", "medium", "available", true))
        .collect(Collectors.toList());

    
    // ********
    // 4. Dynamically create/update pipeline using one of the above ways. Then call dedicated method (@PipelineRun annotated).
    // ********
    // ... create/update Pipeline manually using one of the above methods.
    pipelineStore.update(pipeline);
    ...
    myRestaurant.getMatchingPizzas("MEDIUM");


```
NOTE:
* **Both PipelineStore and PipelineRunner can be easily called from an API like REST.** Example [here](https://github.com/MongoPipe/Examples/tree/main/src/main/java/org/test/sample_spring_boot/controller).
* Store obtained via `Pipelines.getStore()` can be used also to create, update and delete pipelines.
* You can also parameterize an entire pipeline stage/subparts of a stage and send an object, array, pojo or bson type as an actual parameter. For example
   when sorting on multiple fields the `{field1: 1, field2: -1}` can be provided as a Java map or pojo(e.g. org.bson.conversions.Bson) class.
* The pipelines are cached so when running a pipeline that cache is hit first. The cache is automatically updated when you modify the pipelines via the API.
* Once a pipeline is modified (with `PipelineStore`) you can continue to use both `@PipelineRun` annotated methods and`PipelineRunner` to run it.
* As parameter values you can use not just plain Java types (int, long, float, strings, Map, List) but also any POJO class of your choice. 
  The POJO class needs just getters and setters in order to be convertible to JSON (`BsonUtil#toBsonValue` beeing called on it).

# Migration
The migration will be started automatically on process start if using Spring framework (*mongopipe-spring* dependency required) or manually
by invoking:
```java
Pipelines.startMigration();
```
How pipeline changes are detected:
1. **Fast check**: A fast global checksum is created from the `lastModifiedTime` of all incoming pipelines and this checksum is compared with the existing one in the db (saved in the previous run).
2. **Deep check**: If the global checksum based on timestamp matches then skip migration. Else iterate on each incoming pipeline, create a checksum based on content, compare it with the existing checksum in db(saved in a previous run) and create/update db pipeline if content checksums do not match. Also save latest checksums at the end. <br>
The prior value of an updated pipeline will be saved in the `pipeline_store_history` collection for backup purposes. This is configurable. <br>
Default pipeline migration golden source is `src/main/resources/pipelines` (configurable in step 1 
[configuration](#1-configuration) via `MongoPipeConfig#migrationConfig#pipelinesPath`) so store your pipelines json files in that folder. <br>
The pipelines golden source is also configurable in case you want to keep track of the original pipelines within a database instead of a resources folder:
```java
RunContextProvider.getContext().setPipelineMigrationSource(yourDbPipelineSource);
```
Still remember that the final destination of the pipelines (after migration), is the `pipeline_store` db collection where you can update them any time at runtime.<br>
A pipeline that was modified at runtime (in the pipeline store) but did not had the golden source updated will not be overwritten on migration.
It will only be overwritten when the corresponding pipeline is updated in the golden source.

# CRUD stores
**NOTE: This is subject to change.** <br>
A @Store annotated interface can support both @PipelineRun methods and also CRUD methods by naming convention.<br>
For CRUD methods by naming convention (similar with Spring Data) the method signature must match one of the methods from `org.mongopipe.core.store.CrudStore`.
 E.g.:
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
1. The store(via the `@Store` annotation) decides where to put the items and not vice versa, meaning an item type class is STORAGE AGNOSTIC.
   Thus, the annotation `@Store#items` field acts as a database **mapping definition**.
   Another benefit of this mapping definition is that it can be stored in db or in a file, referenced by id, or defaulted to all stores.
2. This feature is not yet mature, main feature is to manage and run pipelines.

# More examples
Find more examples in samples [repo](https://github.com/MongoPipe/Examples).

# Update operations
For performing data updates: 
1. Without pipelines, you can use [CRUD stores](#crud-stores)
2. With pipelines, using [update stages](https://www.mongodb.com/docs/manual/tutorial/update-documents-with-aggregation-pipeline/) like for example the `$replaceRoot`.
3. With pipelines, using dedicated commands like for example [findOneAndUpdate](findOneAndUpdate()) which can be run by setting `Pipeline#commandOptions`.
   The findOneAndUpdate allows also to insert the document if it does not exist.

# Docs
Main documentation site: https://www.mongopipe.org <br>
[JavaDoc](https://javadoc.io/doc/org.mongopipe/mongopipe-core/latest/index.html).

# TODO
- Features requested by users. This is the most important.
- Use MongoDB ChangeStreams to automatically update the optional pipeline cache backing the pipeline store. This is for when pipelines are updated by another external process.
  Now a manual call is needed in case of external modifications: PipelineStore#refresh
- Documentation with Hugo.

# Support and get in touch
<img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/settings-outline.svg" width="20"/><img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/bug-outline.svg" width="20"/> 
Contributions and suggestions are welcomed from everyone. If you have a bug/proposal just contact us directly or browse the open issues and create a new one. <br>
<img src="https://github.com/ionic-team/ionicons/blob/main/src/svg/mail-outline.svg" width="20"/> We like direct discussions. Check email address on the github profile of the committers.


