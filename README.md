# Cauldron
Netty Based Server + Mini Framework for writing Java Based Containers

## Cauldron Http Server
Cauldron Provides a basic Http Implementation backed by Netty Components and a Facade API which abstracts
the underlying implementation from the interface which programmers interact with.

### Resources
Resources are classes which host a set of endpoints under a specific path. Resources are annotated with the `@NetResource`
annotation:
```java
@NetResource(route = "/path")
public class MyResource {
  
}
```
### Endpoints

Endpoints are functions within resources which can host a specific http request to the resource.

#### GET Example
```java
@NetResource(route = "/path")
public class MyResource {
   @HttpEndpoint(method = CauldronHttpMethod.GET)
   public Taffy get(@QueryParam(name = "id", validator = TestStringInputValidator.class) final String id) {
     String myRequestURI = InvocationContext.getRequestContext().getRequestURI();
     LOG.info("get() " + myRequestURI + " with id: " + id);
     return new Taffy();
   } 
}
```
For GET based requests like the one above we use `@QueryParam` and `@HttpEndpoint` annotations to extract what we need from the
incoming cauldron request.
#### Action Example
```java
@NetResource(route = "/path")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.ACTION)
  public void action(@BodyParam(name = "a") final int a, @BodyParam(name = "b") final long b) {
    // perform an action
  }
}
```

#### Non Blocking Resources
For Asynchronous or parallel operations your endpoints can also return a CompletionStage instance. In general
API Containers should be non-blocking implementations since they tend to create downstream traffic and 
will require parallel computation. For mid-tier or backend containers, its fine to use synchronous implementations
when, for instance talking with the database service, etc.
 
```java
@NetResource(route = "/pathAsync")
public class MyAsyncResource {
  @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
  public CompletableFuture<Taffy> get(@QueryParam(name = "id", validator = TestStringInputValidator.class) final String id) {
    CompletableFuture<Taffy> future = new CompletableFuture<>();
    executorService.submit(() -> {
      try {
        Thread.sleep(100);
        String myRequestURI = InvocationContext.getRequestContext().getRequestURI();
        LOG.info("get() " + myRequestURI + " with id: " + id);
      } catch (InterruptedException ex) {
        // nada
      }
      future.complete(new Taffy(id));
    });

    return future;
  }
}
```

For ACTION or other POST / PUT based requests, we use `@BodyParam` in place of `@QueryParam` to extract variables from the incoming request body.

### Endpoint Methods
Cauldron Break Http Methods down into more granular actions. 
For example, `HttpMethod.GET` is broken down into `EndpointHttpMethod.GET`, `EndpointHttpMethod.GET_ALL`, and `EndpointHttpMethod.BATCH_GET`
which tailor to specific request / response scenarios.

