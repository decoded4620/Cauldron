package com.decoded.cauldron.api.network.http;

import com.decoded.cauldron.api.annotation.HttpEndpoint;
import com.decoded.cauldron.api.network.AbstractNetworkResource;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Http Resource class.
 *
 * @see AbstractNetworkResource
 */
public abstract class HttpResource extends AbstractNetworkResource {
  private static final Logger LOG = LoggerFactory.getLogger(HttpResource.class);
  private Map<CauldronHttpMethod, EndpointEntry> httpEndpointEntrypointByHttpMethod = new HashMap<>();

  /**
   * Constructor.
   */
  public HttpResource() {
    @SuppressWarnings("unchecked") Class<? super HttpResource> thisClass = (Class<? super HttpResource>) this.getClass();

    Method[] allMethods = thisClass.getMethods();

    Arrays.stream(allMethods).forEach(classMethod -> {
      if (classMethod.isAnnotationPresent(HttpEndpoint.class)) {
        CauldronHttpMethod httpMethod = classMethod.getAnnotation(HttpEndpoint.class).method();
        LOG.info("Mapping Method " + classMethod.getName() + " -> to HttpMethod: " + httpMethod);
        httpEndpointEntrypointByHttpMethod.put(httpMethod, new EndpointEntry(classMethod, MimeType.APPLICATION_JSON));
      }
    });
  }

  /**
   * Returns the endpoint entry for the cauldron http method.
   *
   * @param httpMethod a {@link CauldronHttpMethod}
   *
   * @return EndpointEntry
   */
  public EndpointEntry getEndpointEntry(CauldronHttpMethod httpMethod) {
    if (httpEndpointEntrypointByHttpMethod.containsKey(httpMethod)) {
      return httpEndpointEntrypointByHttpMethod.get(httpMethod);
    }

    LOG.error("No mapping for HttpMethod: " + httpMethod);
    return null;
  }
}
