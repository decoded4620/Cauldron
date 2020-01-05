# Cauldron
Cauldron is a Mini Framework for writing Java Based Containers.

## Cauldron Http Server
Cauldron Provides a basic Http Server Implementation backed by Netty Components and a Facade API which abstracts
the underlying implementation from the interface which programmers interact with.

### Resources
Resources are classes which expose a set of endpoints under a specific route. Resources are annotated with the `@NetResource` annotation and extend the base ResourceClass for the implementation of cauldron. Here is an example of a  Netty Based resource.
```java
@NetResource(route = "/someObject")
public class MyResource extends NettyHttpNetworkResource {
  
}
```
### Endpoints
Endpoints are functions within resources which can host a specific http request to the resource. Endpoints are annotated at the method and parameter level to extract data from the current request context. The functions are annotated with `@HttpEndpoint`. For example, to expose a basic endpoint at `GET /someObject` which returns a model of type `SomeObject`:
#### Model
```java
@JsonSerialize
public class SomeObject {
  @JsonProperty
  public String id;
  
  public SomeObject(){
  };
  
  public SomeObject(String id){
    this.id = id;
  };
}
```
#### Endpoints
Endpoints are exposed with annotations.

#### `@HttpEndpoint` Annotation
The http endpoint annotation exposes a function to the router using its mapping, and selects a codec based on its mime type. 
##### `method`
The endpoint method allows you to map the same route to different functions by changing the request method header `X-Cauldron-Http-Method`. For a `GET` request, the header can be `GET`, `GET_ALL`, and `BATCH_GET`, allowing three different functions to be mapped for `HttpMethod.GET`. 

For GET based requests like the one above we use `@QueryParam` and `@HttpEndpoint` annotations to extract what we need from the
incoming cauldron request.

##### `responseMimeType`
The Mime Type that the API designates the response to be. This can be different per endpoint and will affect the `Content-Type` header that is sent back to the client. If there is no specified type, the default type will be used. If the client supplies an accept header which does not include this mime type, Cauldron can fallback to a type acceptable to the client if supported.


```java
@NetResource(route = "/someObject")
public class MyResource extends NettyHttpNetworkResource {
   @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
   public SomeObject get(@QueryParam(name="id") final String id) {
     return new SomeObject(id);
   } 
}
```

#### `@QueryParam`
Parameters of the endpoint function can be annotated with `@QueryParam` to map query parameters from the requester to an input parameter to the function.

#### `@BodyParam`
Parameters of the endpoint function can be annotated with `@BodyParam` to map request body parameters from the requester to an input parameter to the function.

#### `@QueryParam` and `@BodyParam` properties
There are properties shared between the annotations which perform the same functions in general. The only difference is how the framework derives the values (either from the query, or the body of the inbound request.)
##### `name`
The name of the query or body parameter from the requester
##### `validator`
the class that will validate the parameter input. This is not required, and by default is a `NoopValidator`.
##### `optional`
If true, this input can be omitted by the requester and the framework will gracefully pass it a `null`.

### Examples of different method mappings
Below are different examples for each Method Type supported by Cauldron
#### GET
Expose a basic endpoint at `GET /someObject`.
```java
@NetResource(route = "/someObject")
public class MyResource extends NettyHttpNetworkResource {
   @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
   public SomeObject get(@QueryParam(name="id") final String someObjectIdentifier) {
     return new SomeObject(someObjectIdentifier);
   } 
}
```
#### GET_ALL
`GET_ALL` is designed to return a list of some data. It can mean getting an entire collection.
```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.GET_ALL, responseMimeType = MimeType.APPLICATION_JSON)
  public List<SomeObject> getAll() {
    return ImmutableList.of(new SomeObject("1"), new SomeObject("2"));
  }
}
```
It can also mean fetching all entities that match some criteria.
```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.GET_ALL, responseMimeType = MimeType.APPLICATION_JSON)
  public List<SomeObject> getAll(@QueryParam(name="matching") String someMatchingCriteria) {
    return ImmutableList.of(new SomeObject("1"), new SomeObject("3"));
  }
}
```
#### BATCH_GET
`BATCH_GET` is designed to return a batch of some data given a set of identifiers. It is returned as a map with each result mapped by its original identifier. This provides a fast way for clients to extract elements from the map and do their own hash based sorting, etc.

```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.BATCH_GET, responseMimeType = MimeType.APPLICATION_JSON)
  public Map<String, SomeObject> batchGet(@QueryParam(name="ids") final List<String> ids) {
    return ids.stream().collect(Collectors.toMap(Function.identity(), id -> new SomeObject(id)));
  }
}
```

#### Action 
`ACTION` is designed as an operation which makes some change to the system (not necessarily a create or an update, but maybe just an operation). Examples of actions can be login or logout.

```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.ACTION)
  public void action(@BodyParam(name = "a") final int a, @BodyParam(name = "b") final long b) {
    // perform an action
  }
}
```

Note: POST operations, such as ACTION based requests use `@BodyParam` in place of `@QueryParam` to extract variables from the incoming request body. This includes PUT, POST, and PATCH operations. Also, by default these operations don't return a result, and are typed `void`. This is not a requirement, however, its possible to actually return an entity from any of the post operations.

#### DELETE
Expose a basic endpoint at `DELETE /someObject`. This deletes the identified object using query parameters to match.

```java
@NetResource(route = "/someObject")
public class MyResource extends NettyHttpNetworkResource {
   @HttpEndpoint(method = CauldronHttpMethod.DELETE)
   public void delete(@QueryParam(name="id") final String someObjectIdentifier) {
     // find and delete the object
   } 
}
```
#### DELETE_ALL
`DELETE_ALL` is designed to delete a list of some data. It can mean removing an entire collection.
```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.DELETE_ALL)
  public void deleteAll() {
    
  }
}
```

#### BATCH_DELETE
`BATCH_DELETE` is designed to delete a batch of some data given a set of identifiers.

```java
@NetResource(route = "/someObject")
public class MyResource {
  @HttpEndpoint(method = CauldronHttpMethod.BATCH_DELETE)
  public void batchDelete(@QueryParam(name="ids") final List<String> ids) {
    return ids.stream().collect(Collectors.toMap(Function.identity(), id -> new SomeObject(id)));
  }
}
```

#### Non Blocking Resources
For Asynchronous or parallel operations your endpoints can also return a CompletionStage instance. In general
BFF Architecture, the API containers should be non-blocking implementations since they tend to create downstream traffic and 
will require parallel computation. For mid-tier or backend containers, its fine to use synchronous implementations
when, for instance talking with the database service, etc.
 
```java
@NetResource(route = "/someObjectAsync")
public class MyAsyncResource {
  @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
  public CompletableFuture<SomeObject> get(@QueryParam(name = "id", validator = AValidator.class) final String id) {
    return CompletableFuture.completedFuture(new SomeObjecvt(id));
  }
}
```
