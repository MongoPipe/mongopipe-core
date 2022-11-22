# mongopipe-core

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/MongoPipe/mongopipe-core/blob/master/LICENSE)
[![Open Source](https://badges.frapsoft.com/os/v3/open-source.svg?v=103)](https://opensource.org/)

# Intro
![logo](docs/vs.png ) <br>
**Forget hardcoding** of MongoDB aggregation pipelines into your Java code. Code is static. Business rules are dynamic. <br> 
A Mongo **pipeline is a BSON document, so store it in the database like every other BSON document.** <br> 
Usage examples:
* You are doing fraud detection using pipelines, a DBA might like to tune **urgently** some pipelines rules according to a newly detected fraud risk.
* You have an UI and a client or administrator wants to change **easily** the values displayed by a dynamic combo box(pipeline backed), or to add new chart(pipeline backed) without waiting for a dedicated release with the new functionality.
* You have multiple reports backed by materialized views or pipelines. You want to easily change the reports via API. 

Parameterized pipelines running, dynamic pipeline management, versioning and automatic migration are supported. <br>
MongoDB pipelines can be used for both **querying and updating** the data.

## My first pipeline.

### 1. Configuration
```java
Stores.registerConfig(MongoPipeConfig.builder()
  .uri("<mongo uri>")
  .databaseName("<database name>")
  //.mongoClient(optionallyForCustomConnection)
  .build());
```

### 2. Create your own interface with `@PipelineRun` methods:
```java
@Store
public interface MyRestaurant {
    @PipelineRun("getPizzaOrdersBySize") // Optional
    Stream<PizzaOrders> getPizzaOrdersBySize(String pizzaSize);      
}    

// Running. 
// A. Without Spring:
Stores.from(MyRestaurant.class)
    .getPizzaOrdersBySize("MEDIUM");

// B. With Spring (mongopipe-spring):
@Autowired
MyRestaurant myRestaurant; // No need to call 'Stores.from'.
...
myRestaurant.getPizzaOrdersBySize("MEDIUM", ...);    
```
**NOTE**: 
1. Alternatively you can use the `Pipelines.getRunner().run` to run any pipeline in your store in a generic way without the need for 
you to create pipeline running interfaces. This is useful in specific scenarios likes the ones in the Intro section.<br>
[Generic creation and running](README.md#Dynamic-creation-and-running)
2. By default the parameters actual values provided are expected to be in the same order in the pipeline template. If that is not the case 
   then annotate the method parameter like this: `List<Pizza> matchingPizzasBySize(@Param("pizzaSize") String pizzaSize)`.
3. Secondary functionality to running database stored pipelines is to derive and run a CRUD pipeline just from the method naming similar 
   with Spring Data, e.g. `Optional<Pizza> findById(String id)`. This happens if @PipelineRun is missing on the method. This is not primary 
   functionality and not fully supported.

### 3. Create resource file `myFirstPipeline.bson`
```bson
{
 "id": "pizzaOrdersBySize",
 "collection": "pizzaCollection",
 "pipeline":
  [
    {
       $match: { size: "${pizzaSize}" }
    },
    {
       $group: { _id: "$name", totalQuantity: { $sum: "$quantity" } }
    }
 ]
}
```
NOTE:
1. The file above although static it is input into the migration utility at process startup and thus seeded in the database. It can then be
   updated at runtime via the PipelineStore (`Pipelines.getStore()`) API or the file can be manually modified and on process startup will be 
   automatically propagated to the database by the migration process. More on (README.md#Migration).    
1. The parameters form is `"${paramName}"`. E.g. `"${pizzaSize}"` above. Inside the template both strings, integers, dates, subdocuments, 
   etc will be enclosed in quotes like a string i.e  `"price": "${pizzaPriceIsANumber}". At runtime that string will be replaced with the
   actual type of the parameter value. This quotes enclosed notation is needed in order for the template to be a valid BSON document.
   

TODO: Structure the content better here.
**In the raw pipeline mark your runtime parameters with: `$paramName`**. <br>
Store the pipeline.bson in a code static that **will be saved automatically in the database** at startup using org.mongopipe.core.migration
(similar to other SQL org.mongopipe.core.migration tools). <br>
 Store the `<my_pipeline>.bson` file in a dedicated "pipelines" code resources folder (e.g. `src/main/resources/pipelines`). <br>

The pipeline params will get replaced in the order they are provided or optionally you can match them by name if you use @Param method parameter annotation. e.g.:
`
@PipelineRun("calculateComplexPizzaReport")
Stream<Report> calculateComplexPizzaReport(@Param("pizzaSize") String size, @Param("startDate") Date startDate, ...);
`



## Dynamic creation and running
If you do not want to use an interface to define the pipeline run methods you can instead manually create and run them:
```java
    // Create pipeline
    Bson matchStage = match(and(eq("size", "$size"), eq("available", "$available"))); // Static imports from com.mongodb.client.model.Aggregates / Filters
    Bson sortByCountStage = sort(descending("price"));
    // Alternatively use BsonUtil.toPojo(bsonString, Pipeline.class) to load pipeline from String.
    Stores.getPipelineStore().createPipeline(Pipeline.builder()        
        .id("dynamicPipeline")
        .pipeline(asList(matchStage, sortByCountStage))
        .collection("testCollection")
        .build());
    PipelineRunner pipelineRunner = Pipelines.getRunner();

    // Run
    List<Pizza> pizzas = pipelineRunner.run("dynamicPipeline", Pizza.class, 
        Maps.of("size", "medium", "available", true))
        .collect(Collectors.toList());
```
NOTE: 
1. You can also load the pipeline json from a file or JSON/BSON string by using: `BsonUtil.toPojo(bsonString, Pipeline.class)`
2. Store obtained via `Stores.getPipelineStore()` can be used also to create, update and delete pipelines. 
3. You can also parameterize an entire pipeline stage/subparts of a stage and send a bson/pojo as an actual parameter to replace it. 
   TODO: Add test where an entire stage or subpart of a stage (e.g. sortCriterias) are sent in as a field (pojo type or bson) and replacing
   takes place inside BsonParameterEvaluator.


# Migration


...

Extra topics:
Pagination pipeline example
Sorting pipeline




## Spring profiles (TODO: move in dedicated spring library)
MongoPipe handles Spring's org.springframework.context.annotation.Profile annotation. If an interface with pipelines is annotated with @Profile, then it will be activated for that profile.

```java
@Profile("uat")
public interface RestaurantReports {
  @PipelineRun("getPizzaReport")
  Stream<Report> calculateComplexPizzaReport(String size, Date startDate, ...);

}
public void devEnvOnly(DB db){
// ...
}
```

### Performing updates using pipelines.
Pipelines are mostly used for queries, but they can be used also for updating data. There are 2 solutions:
1. Using [update stages](https://www.mongodb.com/docs/manual/tutorial/update-documents-with-aggregation-pipeline/) like for example the `$replaceRoot`. 
2. Using dedicated commands like for example [findOneAndUpdate](findOneAndUpdate()) which can be run by setting `Pipeline#commandOptions`. 
   The findOneAndUpdate allows also to insert the document if it does not exist.

# TODO
1. Add example pipeline for pagination, for dynamic criteria.


