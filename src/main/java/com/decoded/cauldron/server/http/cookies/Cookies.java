package com.decoded.cauldron.server.http.cookies;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cauldron Http Cookies.
 */
public class Cookies {
  private static final Logger LOG = LoggerFactory.getLogger(Cookies.class);

  private final Map<String, Cookie> cookies = new HashMap<>();

  private Cookies() {

  }

  /**
   * Parses the cookie headers sent form the client.
   *
   * @param cookieHeaderValues a Set of strings (the cookie headers)
   *
   * @return a {@link Cookies}
   */
  public static Cookies parse(Set<String> cookieHeaderValues) {
    Cookies cookies = new Cookies();

    // The cookie header values are passed in by the client, in the format
    // name=x; name2=y;
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie
    cookieHeaderValues.stream().forEach(cookieHeader -> {
      Set<Cookie> cookieSet = Cookie.parseClientCookie(cookieHeader);
      cookieSet.forEach(cookie -> cookies.addParsedCookie(cookie.getName(), cookie));
    });

    return cookies;
  }

  public Map<String, Cookie> getCookies() {
    return ImmutableMap.copyOf(cookies);
  }

  public Cookie getCookie(String cookieName) {
    return cookies.get(cookieName);
  }

  private void addParsedCookie(String name, Cookie cookie) {
    Cookie actual = cookies.putIfAbsent(name, cookie);
    // if these are not the same cookie instance
    if (actual != cookie) {
      LOG.warn("Set-Cookie name: " + name + " is duplicated");
    }
  }
}
