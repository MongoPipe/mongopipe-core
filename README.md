# mongopipe-core

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/MongoPipe/mongopipe-core/blob/master/LICENSE)
[![Open Source](https://badges.frapsoft.com/os/v3/open-source.svg?v=103)](https://opensource.org/)

![logo](docs/mongopipe.gif)

# Intro

**Forget hardcoding** of MongoDB aggregation pipelines into your code. Code is static. <br>
A **pipeline is a BSON document, so store it in the database.** <br>
Usage examples:
* You are doing fraud detection using pipelines, a DBA might like to tune **urgently** some pipelines according to a newly detected fraud risk.
* You have an UI and a client or administrator wants to change **easily** the values displayed by a dynamic combo box(pipeline backed), or to add new chart(pipeline backed) without waiting for a dedicated release with the new functionality.

Parameterized pipelines running, dynamic pipeline management, versioning and automatic org.mongopipe.core.migration are supported. <br>
MongoDB pipelines can be used for both **querying and updating** the data.

## My first pipeline.

### 1. Configuration
```java
Pipelines.newConfig()
  .uri("<mongo uri>")
  .databaseName("<database name>")
  .build();
```

### 2. `@Pipeline` annotation:
```java
@PipelineRepository
public interface MyRestaurant {
    @Pipeline("getPizzaOrdersBySize")
    Stream<PizzaOrders> getPizzaOrdersBySize(@Param("pizzaSize") String pizzaSize);
     
    @Pipeline("calculateComplexPizzaReport")
    Stream<Report> calculateComplexPizzaReport(String size, Date startDate, ...); 
}    

 
// A. Without Spring (mongopipe-core):
Pipelines.from(MyRestaurant.class)
    .getPizzaOrdersBySize("MEDIUM");

// B. With Spring (mongopipe-spring):
@Autowired
MyRestaurant myRestaurant; // No need to call 'Pipelines.from'.
...
myRestaurant.calculateComplexPizzaReport("MEDIUM", ...);    
```
NOTE: Alternatively you can use the `Pipelines.getRunner` to run any pipeline in your store in a generic way. More on `PipelineRunner`(TODO: documentation link here).


### 3. `myFirstPipeline.bson` pipeline
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
**In the raw pipeline mark your runtime parameters with: `${paramName}`**. <br>
The parameter name in the raw pipeline must match the method parameter with the **@Param("paramName")** annotation as in step 1.

If you want to reuse the same raw pipeline within multiple executions you can use the **${pipeline.import}** directive.

You have multiple ways of creating the pipeline:
1. Using code files as seeds that **will be saved automatically in the database** at startup using org.mongopipe.core.migration (similar to other SQL org.mongopipe.core.migration tools). <br>
   Store the `<my_pipeline>.bson` file in a dedicated "pipelines" code resources folder (e.g. `src/main/resources/pipelines`). <br>
   Use the `Pipelines.newConfig().pipelinesPath(...)` builder method to set the location path/uri.

2. `Pipelines#getStore().create(pipelinePojo)`to create at runtime. For example this can be called from an endpoint.
   ```java
   Pipelines#getStore().create(RunPipeline.builder()
      .id("pizzaReport")
      .collection("pizzaCollection")
      .pipeline("pipeline template")
      .build());
   ```
   Similarly you can use the store to update pipeline at runtime.

NOTE:
* As you observed the pipeline model includes also runtime information like collection on which to be run and operation type. <br>
  If users need it, a @RunContext annotation could be added for passing run context from code.




...
Extra topics:
Pagination pipeline example
Sorting pipeline

## Spring profiles (TODO: move in dedicated spring library)
MongoPipe handles Spring's org.springframework.context.annotation.Profile annotation. If an interface with pipelines is annotated with @Profile, then it will be activated for that profile.

```java
@Profile("uat")
public interface RestaurantReports {
  @Pipeline("getPizzaReport")
  Stream<Report> calculateComplexPizzaReport(String size, Date startDate, ...);

}
public void devEnvOnly(DB db){
// ...
}
```