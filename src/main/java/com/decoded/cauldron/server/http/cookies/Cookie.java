package com.decoded.cauldron.server.http.cookies;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Cauldron Cookie Class.
 */
public class Cookie {
  private String name = "";
  private String value = "";
  private long maxAge = Long.MIN_VALUE;
  private String path = null;
  private String domain = null;
  private boolean secure = false;
  private boolean httpOnly = false;
  private SameSite sameSite = null;

  private Cookie() {

  }

  /**
   * Create a new cookie.
   *
   * @param name  the name
   * @param value the value
   *
   * @return a {@link Cookie}
   */
  public static Cookie create(String name, String value) {
    Cookie cookie = new Cookie();
    cookie.name = name;
    cookie.value = value;

    return cookie;
  }

  /**
   * Create a client cookie.
   *
   * @param name     the name
   * @param value    the value
   * @param path     the path
   * @param domain   the cookie domain
   * @param secure   secure flag
   * @param httpOnly http only flag
   * @param sameSite same site setting
   * @param maxAge   max age (in ms)
   * @param expires  expiration date
   *
   * @return a {@link Cookie}
   */
  public static Cookie create(String name,
                              String value,
                              String path,
                              String domain,
                              boolean secure,
                              boolean httpOnly,
                              SameSite sameSite,
                              long maxAge,
                              long expires) {
    Cookie cookie = create(name, value);

    cookie.path = path;
    cookie.domain = domain;
    cookie.secure = secure;
    cookie.httpOnly = httpOnly;
    cookie.sameSite = sameSite;
    cookie.maxAge = maxAge;

    return cookie;
  }

  /**
   * Parses a client cookie (from the agent).
   *
   * @param cookieString the cookie string
   *
   * @return a Set of cookies.
   */
  public static Set<Cookie> parseClientCookie(String cookieString) {
    Set<Cookie> cookieSet = new HashSet<>();

    StringTokenizer tokenizer = new StringTokenizer(cookieString, ";");
    String[] tokens = new String[tokenizer.countTokens()];
    int idx = 0;
    while (tokenizer.hasMoreTokens()) {
      tokens[idx++] = tokenizer.nextToken();
    }

    if (tokens.length > 0) {
      if (tokens.length % 2 != 0) {
        // error
      }

      for (int i = 0; i < tokens.length; i += 2) {
        Cookie cookie = new Cookie();
        cookie.name = tokens[i];
        cookie.value = tokens[i + 1];
        cookieSet.add(cookie);
      }
    }

    return cookieSet;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public long getMaxAge() {
    return maxAge;
  }


  public String getPath() {
    return path;
  }

  public String getDomain() {
    return domain;
  }

  public SameSite getSameSite() {
    return sameSite;
  }

  public boolean isSecure() {
    return secure;
  }

  public boolean isHttpOnly() {
    return httpOnly;
  }
}
