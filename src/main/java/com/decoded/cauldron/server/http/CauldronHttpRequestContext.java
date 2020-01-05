package com.decoded.cauldron.server.http;

import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.HttpMethod;
import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.server.http.cookies.Cookie;
import com.decoded.cauldron.server.http.cookies.Cookies;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface CauldronHttpRequestContext {

  Set<String> getRequestHeaders(String headerName);

  Map<String, Set<String>> getRequestHeaders();

  void addResponseHeader(String headerName, String value);

  Cookie getClientCookie(String headerName);

  void addClientCookie(Cookie cookie);

  Cookies getClientCookies();

  String getRequestUri();

  String getRequestPath();

  String getQueryString();

  String getQueryParameter(String parameterName);

  Map<String, List<String>> getQueryParameters();

  List<String> getQueryParameters(String parameterName);

  String getBodyParameter(String parameterName);

  List<String> getBodyParameters(String parameterName);

  Map<String, List<String>> getBodyParameters();

  String getResponseContentMD5();

  Object getResult();

  void setResult(Object result);

  HttpMethod getRequestMethod();

  CauldronHttpMethod getCauldronRequestMethod();

  CryptographyService getCryptographyService();
}
