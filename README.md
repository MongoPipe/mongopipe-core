# mongopipe-core

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/MongoPipe/mongopipe-core/blob/master/LICENSE)
[![Open Source](https://badges.frapsoft.com/os/v3/open-source.svg?v=103)](https://opensource.org/)

# Intro
![logo](docs/vs.png ) <br>
**Forget hardcoding** of MongoDB aggregation pipelines into your code. Code is static. Business rules are dynamic. <br> 
A **pipeline is a BSON document, so store it in the database.** <br> 
Usage examples:
* You are doing fraud detection using pipelines, a DBA might like to tune **urgently** some pipelines according to a newly detected fraud risk.
* You have an UI and a client or administrator wants to change **easily** the values displayed by a dynamic combo box(pipeline backed), or to add new chart(pipeline backed) without waiting for a dedicated release with the new functionality.
* You have multiple reports backed by materialized views or pipelines. You want to easily change the reports via API. 

Parameterized pipelines running, dynamic pipeline management, versioning and automatic org.mongopipe.core.migration are supported. <br>
MongoDB pipelines can be used for both **querying and updating** the data.

## My first pipeline.

### 1. Configuration
```java
Pipelines.newConfig()
  .uri("<mongo uri>")
  .databaseName("<database name>")
  //.mongoClient(optionalYourMongoClientInstance)
  .build();
```

### 2. Your own `@PipelineRunners`:
```java
@PipelineRunners
public interface MyRestaurant {
    @PipelineRun("getPizzaOrdersBySize")
    Stream<PizzaOrders> getPizzaOrdersBySize(@Param("pizzaSize") String pizzaSize);
     
    @PipelineRun("calculateComplexPizzaReport")
    Stream<Report> calculateComplexPizzaReport(@Param("pizzaSize") String size, @Param("startDate") Date startDate, ...); 
}    

 
// A. Without Spring (mongopipe-core):
Pipelines.from(MyRestaurant.class)
    .getPizzaOrdersBySize("MEDIUM");

// B. With Spring (mongopipe-spring):
@Autowired
MyRestaurant myRestaurant; // No need to call 'Pipelines.from'.
...
myRestaurant.getPizzaOrdersBySize("MEDIUM", ...);    
```
**NOTE**: Alternatively you can use the `Pipelines.getStore().run` to run any pipeline in your store in a generic way without the need for 
you to create pipeline running interfaces. This is useful in specific scenarios likes the ones in the Intro section.<br>
[Generic creation and running](README.md#Generic-creation-and-running)

### 3. `myFirstPipeline.bson` pipeline
```bson
{
 "id": "pizzaOrdersBySize",
 "collection": "pizzaCollection",
 "pipeline":
  [
    {
       $match: { size: "$pizzaSize" }
    },
    {
       $group: { _id: "$name", totalQuantity: { $sum: "$quantity" } }
    }
 ]
}
```
TODO: Structure the content better here.
**In the raw pipeline mark your runtime parameters with: `$paramName`**. <br>
The parameter name in the raw pipeline must match the method parameter with the **@Param("paramName")** annotation as in step 1.
Store the pipeline.bson in a code static that **will be saved automatically in the database** at startup using org.mongopipe.core.migration
(similar to other SQL org.mongopipe.core.migration tools). <br>
 Store the `<my_pipeline>.bson` file in a dedicated "pipelines" code resources folder (e.g. `src/main/resources/pipelines`). <br>
Use the `Pipelines.newConfig().pipelinesPath(...)` builder method to set the location path/uri.


## Manual creation and running
If you do not want to use an interface to define the pipeline run methods you can instead manually create and run them:
```java
    // Create pipeline
    Bson matchStage = match(and(eq("size", "$size"), eq("available", "$available"))); // Static imports from com.mongodb.client.model.Aggregates / Filters
    Bson sortByCountStage = sort(descending("price"));
    // Alternatively use BsonUtil.toPojo(bsonString, Pipeline.class) to load pipeline from String.
    Pipelines.getStore().createPipeline(Pipeline.builder()
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
2. PipelineStore obtained via `Pipelines.getStore()` can be used also to create, update and delete pipelines. 
3. You can also parameterize an entire pipeline stage/subparts of a stage and send a bson/pojo as an actual parameter to replace it. 
   TODO: Add test where an entire stage or subpart of a stage (e.g. sortCriterias) are sent in as a field (pojo type or bson) and replacing
   takes place inside BsonParameterEvaluator.

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

# TODO
1. Add example pipeline for pagination, for dynamic criteria.

