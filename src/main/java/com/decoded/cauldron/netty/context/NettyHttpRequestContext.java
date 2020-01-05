package com.decoded.cauldron.netty.context;


import com.decoded.cauldron.api.CauldronApi;
import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.HeaderNames;
import com.decoded.cauldron.api.network.http.HttpMethod;
import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.netty.handler.codec.http.EnhancedCookie;
import com.decoded.cauldron.netty.handler.codec.http.EnhancedServerCookieEncoder;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.http.CauldronHttpRequestContext;
import com.decoded.cauldron.server.http.cookies.Cookie;
import com.decoded.cauldron.server.http.cookies.Cookies;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import io.netty.util.CharsetUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Request Context, which contains thread local access to the current execution context for the current incoming request that is being handled.
 */
public class NettyHttpRequestContext implements CauldronHttpRequestContext {
  private static final Logger LOG = LoggerFactory.getLogger(NettyHttpRequestContext.class);

  private static final Map<io.netty.handler.codec.http.HttpMethod, HttpMethod> methodToHttpMethodMap;

  static {
    Map<io.netty.handler.codec.http.HttpMethod, HttpMethod> map = new HashMap<>();
    map.put(io.netty.handler.codec.http.HttpMethod.GET, HttpMethod.GET);
    map.put(io.netty.handler.codec.http.HttpMethod.CONNECT, HttpMethod.CONNECT);
    map.put(io.netty.handler.codec.http.HttpMethod.POST, HttpMethod.POST);
    map.put(io.netty.handler.codec.http.HttpMethod.PATCH, HttpMethod.PATCH);
    map.put(io.netty.handler.codec.http.HttpMethod.PUT, HttpMethod.PUT);
    map.put(io.netty.handler.codec.http.HttpMethod.DELETE, HttpMethod.DELETE);
    map.put(io.netty.handler.codec.http.HttpMethod.OPTIONS, HttpMethod.OPTIONS);
    map.put(io.netty.handler.codec.http.HttpMethod.HEAD, HttpMethod.HEAD);
    map.put(io.netty.handler.codec.http.HttpMethod.TRACE, HttpMethod.TRACE);

    methodToHttpMethodMap = Collections.unmodifiableMap(map);
  }

  private HttpRequest request;
  private HttpResponse response;
  private List<Cookie> addedCookies = new ArrayList<>();
  private Object result;
  private ChannelHandlerContext ctx;
  private CryptographyService cryptographyService = null;

  /**
   * Constructor.
   *
   * @param ctx the {@link ChannelHandlerContext}
   */
  public NettyHttpRequestContext(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  public ChannelHandlerContext getCtx() {
    return ctx;
  }

  @Override
  public CryptographyService getCryptographyService() {
    return cryptographyService;
  }

  /**
   * Set the cryptography Service.
   *
   * @param cryptographyService a {@link CryptographyService} instance.
   *
   * @return this context.
   */
  public NettyHttpRequestContext setCryptographyService(final CryptographyService cryptographyService) {
    this.cryptographyService = cryptographyService;
    return this;
  }

  /**
   * Set the {@link HttpRequest}.
   *
   * @param request the request.
   *
   * @return this context.
   */
  public NettyHttpRequestContext setRequest(final HttpRequest request) {
    this.request = request;
    return this;
  }

  @Override
  public Object getResult() {
    return result;
  }

  @Override
  public void setResult(final Object result) {
    this.result = result;
  }

  /**
   * Constructor.
   *
   * @param response the Http response.
   *
   * @return a Netty Http Request context.
   */
  public NettyHttpRequestContext setResponse(HttpResponse response) {
    this.response = response;
    // if there were pending cookies..
    if (this.response != null) {
      if (!addedCookies.isEmpty()) {
        addedCookies.forEach(this::addClientCookie);
        addedCookies.clear();
      }
    }

    return this;
  }

  @Override
  public Cookies getClientCookies() {
    return Cookies.parse(getRequestHeaders(HeaderNames.COOKIE));
  }

  @Override
  public Cookie getClientCookie(final String clientCookieName) {
    return getClientCookies().getCookie(clientCookieName);
  }

  @Override
  public void addClientCookie(final Cookie cookie) {
    if (cookie == null) {
      LOG.error("Cannot add null cookie");
      return;
    }
    if (response == null) {
      addedCookies.add(cookie);
    } else {
      if (response instanceof FullHttpResponse) {
        EnhancedCookie enhancedCookie = EnhancedServerCookieEncoder.formatCookie(cookie);
        response.headers().add(HeaderNames.SET_COOKIE, EnhancedServerCookieEncoder.LAX.encode(enhancedCookie));
      }
    }
  }

  @Override
  public Set<String> getRequestHeaders(final String headerName) {
    Set<String> results = new HashSet<String>();
    request.headers().forEach(e -> {
      if (e.getKey().equals(headerName)) {
        results.add(e.getValue());
      }
    });
    return results;
  }

  @Override
  public Map<String, Set<String>> getRequestHeaders() {
    Map<String, Set<String>> results = new HashMap<>();
    request.headers().forEach((mapEntry) -> results.put(mapEntry.getKey(), getRequestHeaders(mapEntry.getKey())));
    return results;
  }

  @Override
  public String getQueryString() {
    final String uri = request.uri();
    final int queryStringIdx = uri.indexOf('?');
    return queryStringIdx > -1 ? uri.substring(queryStringIdx + 1) : "";
  }

  @Override
  public List<String> getBodyParameters(final String parameterName) {
    if (request == null) {
      return Collections.emptyList();
    }

    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
    try {
      List<InterfaceHttpData> postList = decoder.getBodyHttpDatas();
      List<String> bodyParams = new ArrayList<>();
      for (InterfaceHttpData data : postList) {
        if (data.getName().equals(parameterName)) {
          List<String> values = new ArrayList<>();
          MixedAttribute value = (MixedAttribute) data;
          value.setCharset(CharsetUtil.UTF_8);
          values.add(value.getValue());
          bodyParams.addAll(values);
        }
      }

      decoder.destroy();
      return bodyParams;
    } catch (Exception e) {
      LOG.error("Error getting body parameter values for: " + parameterName, e);
    }

    return Collections.emptyList();
  }

  @Override
  public Map<String, List<String>> getBodyParameters() {
    if (request == null) {
      return Collections.emptyMap();
    }

    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
    try {
      List<InterfaceHttpData> postList = decoder.getBodyHttpDatas();
      Map<String, List<String>> bodyParamsMap = new HashMap<>();
      for (InterfaceHttpData data : postList) {

        List<String> values = new ArrayList<>();
        MixedAttribute value = (MixedAttribute) data;
        value.setCharset(CharsetUtil.UTF_8);
        values.add(value.getValue());

        bodyParamsMap.computeIfAbsent(data.getName(), n -> new ArrayList<>()).addAll(values);
      }

      return bodyParamsMap;
    } catch (Exception e) {
      LOG.error("Error getting body parameter values", e);
    }

    return null;
  }

  @Override
  public String getBodyParameter(final String parameterName) {
    List<String> bodyParamsForName = getBodyParameters(parameterName);
    if (!bodyParamsForName.isEmpty()) {
      return bodyParamsForName.get(0);
    }

    return null;
  }

  @Override
  public String getQueryParameter(String parameterName) {
    List<String> paramsOfName = getQueryParameters().get(parameterName);
    if (paramsOfName == null || paramsOfName.isEmpty()) {
      LOG.warn("No query parameter of name: " + parameterName + " was found in the request");
      return null;
    }

    return paramsOfName.get(0);
  }

  @Override
  public Map<String, List<String>> getQueryParameters() {
    String qs = getQueryString();
    StringTokenizer tokenizer = new StringTokenizer(qs, "&");
    Map<String, List<String>> queryParams = new HashMap<>();

    try {
      while (tokenizer.hasMoreTokens()) {
        String pairString = tokenizer.nextToken();
        StringTokenizer pair = new StringTokenizer(pairString, "=");

        String key = pair.hasMoreTokens() ? pair.nextToken() : null;
        String value = pair.hasMoreTokens() ? pair.nextToken() : null;

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        queryParams.computeIfAbsent(key, t -> new ArrayList<>());
        queryParams.get(key).add(value);
      }
    } catch (NullPointerException ex) {
      throw new CauldronServerException("Error with query parameters", ex);
    }
    return queryParams;
  }


  @Override
  public List<String> getQueryParameters(String parameterName) {
    List<String> paramsOfName = getQueryParameters().get(parameterName);
    if (paramsOfName == null || paramsOfName.isEmpty()) {
      LOG.warn("No query parameter values of name: " + parameterName + " was found in the request");
      return null;
    }

    return paramsOfName;
  }

  @Override
  public String getRequestPath() {
    final String uri = request.uri();
    final int queryStringIdx = uri.indexOf('?');
    return queryStringIdx > -1 ? uri.substring(0, queryStringIdx) : uri;
  }

  @Override
  public String getRequestUri() {
    return request.uri();
  }

  @Override
  public void addResponseHeader(String headerName, String value) {
    if (value == null) {
      LOG.warn("Can't set header " + headerName + " to NULL");
    } else {
      response.headers().set(headerName, ImmutableSet.of(value));
    }
  }

  @Override
  public HttpMethod getRequestMethod() {
    return methodToHttpMethodMap.get(request.method());
  }

  @Override
  public CauldronHttpMethod getCauldronRequestMethod() {
    return CauldronApi.getRequestEndpointMethod();
  }

  @Override
  public String getResponseContentMD5() {
    ByteBuf buffer;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(result);
      out.flush();
      buffer = Unpooled.wrappedBuffer(bos.toByteArray());
      ByteBufInputStream is = new ByteBufInputStream(buffer);
      String md5 = CauldronApi.computeContentMD5Header(is);
      buffer.release();
      return md5;
    } catch (IOException ex) {
      LOG.error("Error creating bytes", ex);
    }

    return "";
  }
}
