package com.decoded.cauldron.api;

import com.decoded.cauldron.api.annotation.HttpEndpoint;
import com.decoded.cauldron.api.network.http.CauldronHeaderNames;
import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.HttpMethod;
import com.decoded.cauldron.api.network.http.HttpResource;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.http.InvocationContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CauldronApi {
  private static final Logger LOG = LoggerFactory.getLogger(CauldronApi.class);
  private static final Map<CauldronHttpMethod, HttpMethod> endpointMethodToHttpMethodMap;
  private static final Map<HttpMethod, CauldronHttpMethod> httpMethodToDefaultEndpointMethodMap;

  static {
    Map<CauldronHttpMethod, HttpMethod> methodMap = new HashMap<>();
    methodMap.put(CauldronHttpMethod.ACTION, HttpMethod.POST);
    methodMap.put(CauldronHttpMethod.CREATE, HttpMethod.POST);
    methodMap.put(CauldronHttpMethod.GET, HttpMethod.GET);
    methodMap.put(CauldronHttpMethod.UPDATE, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.PARTIAL_UPDATE, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.DELETE, HttpMethod.DELETE);
    methodMap.put(CauldronHttpMethod.GET_ALL, HttpMethod.GET);
    methodMap.put(CauldronHttpMethod.UPDATE_ALL, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.PARTIAL_UPDATE_ALL, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.DELETE_ALL, HttpMethod.DELETE);
    methodMap.put(CauldronHttpMethod.BATCH_GET, HttpMethod.GET);
    methodMap.put(CauldronHttpMethod.BATCH_UPDATE, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.BATCH_PARTIAL_UPDATE, HttpMethod.PUT);
    methodMap.put(CauldronHttpMethod.BATCH_DELETE, HttpMethod.DELETE);
    methodMap.put(CauldronHttpMethod.BATCH_CREATE, HttpMethod.POST);

    endpointMethodToHttpMethodMap = Collections.unmodifiableMap(methodMap);

    Map<HttpMethod, CauldronHttpMethod> epMethodMap = new HashMap<>();
    epMethodMap.put(HttpMethod.POST, CauldronHttpMethod.CREATE);
    epMethodMap.put(HttpMethod.GET, CauldronHttpMethod.GET);
    epMethodMap.put(HttpMethod.PUT, CauldronHttpMethod.UPDATE);
    epMethodMap.put(HttpMethod.DELETE, CauldronHttpMethod.DELETE);
    epMethodMap.put(HttpMethod.PATCH, CauldronHttpMethod.PARTIAL_UPDATE);

    httpMethodToDefaultEndpointMethodMap = Collections.unmodifiableMap(epMethodMap);
  }

  private CauldronApi() {

  }

  /**
   * Get the base http method for a {@link CauldronHttpMethod}.
   *
   * @param method the cauldron http method.
   *
   * @return the associated {@link HttpMethod}
   */
  public static HttpMethod getHttpMethod(CauldronHttpMethod method) {
    return endpointMethodToHttpMethodMap.get(method);
  }

  /**
   * Gets the default {@link CauldronHttpMethod} for an {@link HttpMethod}.
   *
   * @param method the {@link HttpMethod}
   *
   * @return a default {@link CauldronHttpMethod}
   */
  public static CauldronHttpMethod getDefaultEndpointHttpMethod(HttpMethod method) {
    return httpMethodToDefaultEndpointMethodMap.get(method);
  }

  /**
   * Returns the {@link CauldronHttpMethod} for the current executing request.
   *
   * @return a {@link CauldronHttpMethod}
   */
  public static CauldronHttpMethod getRequestEndpointMethod() {
    CauldronHttpMethod endpointMethod;
    Set<String> h = InvocationContext.getRequestContext().getRequestHeaders(CauldronHeaderNames.CAULDRON_HTTP_METHOD);

    if (!h.isEmpty()) {
      String headerMethod = h.iterator().next();
      endpointMethod = CauldronHttpMethod.valueOf(headerMethod);
    } else {
      endpointMethod = CauldronApi.getDefaultEndpointHttpMethod(InvocationContext.getRequestContext().getRequestMethod());
    }

    if (endpointMethod == null) {
      LOG.warn("Endpoint Method not found for " + InvocationContext.getRequestContext().getRequestMethod());
    }

    return endpointMethod;
  }

  /**
   * Loads an array of {@link String} representing the allowed Http Methods that a specific resource supports. This is for building the Allow
   * Http Header.
   *
   * @param resource the {@link HttpResource}
   *
   * @return a {@link String} array.
   */
  public static String[] loadAllowedMethodsForResource(HttpResource resource) {
    Method[] declaredResourceMethods = resource.getClass().getDeclaredMethods();
    Set<HttpMethod> allowedMethods = new HashSet<>();
    for (Method m : declaredResourceMethods) {
      if (m.isAnnotationPresent(HttpEndpoint.class)) {
        HttpEndpoint annotation = m.getAnnotation(HttpEndpoint.class);
        allowedMethods.add(CauldronApi.getHttpMethod(annotation.method()));
      }
    }
    if (allowedMethods.isEmpty()) {
      LOG.warn("There were no available HttpMethods allowed for the resource: " + resource.getClass());
    }
    return allowedMethods.stream().map(HttpMethod::name).toArray(String[]::new);
  }

  /**
   * Creates the Content-MD5 header value from the given Input Stream.
   *
   * @param inputStream the content stream.
   */
  public static String computeContentMD5Header(InputStream inputStream) {
    DigestInputStream dis;
    try {
      dis = new DigestInputStream(inputStream, MessageDigest.getInstance("MD5"));
      int bufSize = 4096;
      while (bufSize < inputStream.available()) {
        bufSize *= 2;
      }

      // Consume the stream to compute the MD5 as a side effect.
      byte[] buffer = new byte[bufSize];
      while (dis.read(buffer) > 0) {
      }

      return new String(Base64.encodeBase64(dis.getMessageDigest().digest()), Charsets.UTF_8);
    } catch (NoSuchAlgorithmException ex) {
      LOG.error("Error computing md5", ex);
      throw new CauldronServerException("Error computing the MD5 value for the input stream", ex);
    } catch (IOException ex) {
      LOG.error("Error reading input stream", ex);
      throw new CauldronServerException("Error reading the input stream", ex);
    }
  }
}
