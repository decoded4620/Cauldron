/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.decoded.cauldron.netty.handler.codec.http;

import static com.decoded.cauldron.netty.handler.codec.http.CookieUtil.add;
import static com.decoded.cauldron.netty.handler.codec.http.CookieUtil.addQuoted;
import static com.decoded.cauldron.netty.handler.codec.http.CookieUtil.stringBuilder;
import static com.decoded.cauldron.netty.handler.codec.http.CookieUtil.stripTrailingSeparator;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

import com.decoded.cauldron.server.http.cookies.Cookie;
import com.decoded.cauldron.server.http.cookies.CookieDirectives;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.cookie.CookieEncoder;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A <a href="http://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie encoder to be used server side, so some fields are sent (Version
 * is typically ignored).
 *
 * @see io.netty.handler.codec.http.cookie.ServerCookieEncoder
 */
public final class EnhancedServerCookieEncoder extends CookieEncoder {

  /**
   * Strict encoder that validates that name and value chars are in the valid scope defined in RFC6265, and (for methods that accept multiple
   * cookies) that only one cookie is encoded with any given name. (If multiple cookies have the same name, the last one is the one that is
   * encoded.)
   */
  public static final EnhancedServerCookieEncoder STRICT = new EnhancedServerCookieEncoder(true);

  /**
   * Lax instance that doesn't validate name and value, and that allows multiple cookies with the same name.
   */
  public static final EnhancedServerCookieEncoder LAX = new EnhancedServerCookieEncoder(false);

  private EnhancedServerCookieEncoder(boolean strict) {
    super(strict);
  }

  /**
   * Deduplicate a list of encoded cookies by keeping only the last instance with a given name.
   *
   * @param encoded         The list of encoded cookies.
   * @param nameToLastIndex A map from cookie name to index of last cookie instance.
   *
   * @return The encoded list with all but the last instance of a named cookie.
   */
  private static List<String> dedup(List<String> encoded, Map<String, Integer> nameToLastIndex) {
    boolean[] isLastInstance = new boolean[encoded.size()];
    for (int idx : nameToLastIndex.values()) {
      isLastInstance[idx] = true;
    }
    List<String> dedupd = new ArrayList<String>(nameToLastIndex.size());
    for (int i = 0, n = encoded.size(); i < n; i++) {
      if (isLastInstance[i]) {
        dedupd.add(encoded.get(i));
      }
    }
    return dedupd;
  }

  /**
   * Format a {@link Cookie} into an {@link EnhancedCookie}.
   *
   * @param cauldronCookie a Cookie
   *
   * @return an EnhancedCookie
   */
  public static final EnhancedCookie formatCookie(Cookie cauldronCookie) {
    EnhancedDefaultCookie cookie = new EnhancedDefaultCookie(cauldronCookie.getName(), cauldronCookie.getValue());

    cookie.setDomain(cauldronCookie.getDomain());
    cookie.setHttpOnly(cauldronCookie.isHttpOnly());
    cookie.setSecure(cauldronCookie.isSecure());
    cookie.setPath(cauldronCookie.getPath());
    cookie.setMaxAge(cauldronCookie.getMaxAge());
    cookie.setSameSite(cauldronCookie.getSameSite());

    return cookie;
  }

  /**
   * Encodes the specified cookie name-value pair into a Set-Cookie header value.
   *
   * @param name  the cookie name
   * @param value the cookie value
   *
   * @return a single Set-Cookie header value
   */
  public String encode(String name, String value) {
    return encode(new EnhancedDefaultCookie(name, value));
  }

  /**
   * Encodes the specified cookie into a Set-Cookie header value.
   *
   * @param cookie the cookie
   *
   * @return a single Set-Cookie header value
   */
  public String encode(EnhancedCookie cookie) {
    final String name = checkNotNull(cookie, "cookie").name();
    final String value = cookie.value() != null ? cookie.value() : "";

    validateCookie(name, value);

    StringBuilder buf = stringBuilder();

    if (cookie.wrap()) {
      addQuoted(buf, name, value);
    } else {
      add(buf, name, value);
    }

    if (cookie.maxAge() != Long.MIN_VALUE) {
      add(buf, CookieHeaderNames.MAX_AGE, cookie.maxAge());
      Date expires = new Date(cookie.maxAge() * 1000 + System.currentTimeMillis());
      buf.append(CookieHeaderNames.EXPIRES);
      buf.append('=');
      buf.append(new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'").format(expires));
      buf.append(';');
      buf.append(HttpConstants.SP_CHAR);
    }

    if (cookie.path() != null) {
      add(buf, CookieHeaderNames.PATH, cookie.path());
    }

    if (cookie.domain() != null) {
      add(buf, CookieHeaderNames.DOMAIN, cookie.domain());
    }

    // TODO - fold this back to the default cookie tools in Netty once Same Site / Expires are supported.
    if (cookie.getSameSite() != null) {
      add(buf, CookieDirectives.SAME_SITE, cookie.getSameSite().getValue());
    }

    if (cookie.isSecure()) {
      add(buf, CookieHeaderNames.SECURE);
    }
    if (cookie.isHttpOnly()) {
      add(buf, CookieHeaderNames.HTTPONLY);
    }

    return stripTrailingSeparator(buf);
  }

  /**
   * Batch encodes cookies into Set-Cookie header values.
   *
   * @param cookies a bunch of cookies
   *
   * @return the corresponding bunch of Set-Cookie headers
   */
  public List<String> encode(EnhancedCookie... cookies) {
    if (checkNotNull(cookies, "cookies").length == 0) {
      return Collections.emptyList();
    }

    List<String> encoded = new ArrayList<String>(cookies.length);
    Map<String, Integer> nameToIndex = strict && cookies.length > 1 ? new HashMap<>() : null;
    boolean hasDupName = false;
    for (int i = 0; i < cookies.length; i++) {
      EnhancedCookie c = cookies[i];
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupName |= nameToIndex.put(c.name(), i) != null;
      }
    }
    return hasDupName ? dedup(encoded, nameToIndex) : encoded;
  }

  /**
   * Batch encodes cookies into Set-Cookie header values.
   *
   * @param cookies a bunch of cookies
   *
   * @return the corresponding bunch of Set-Cookie headers
   */
  public List<String> encode(Collection<? extends EnhancedCookie> cookies) {
    if (checkNotNull(cookies, "cookies").isEmpty()) {
      return Collections.emptyList();
    }

    List<String> encoded = new ArrayList<String>(cookies.size());
    Map<String, Integer> nameToIndex = strict && cookies.size() > 1 ? new HashMap<>() : null;
    int i = 0;
    boolean hasDupdName = false;
    for (EnhancedCookie c : cookies) {
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupdName |= nameToIndex.put(c.name(), i++) != null;
      }
    }
    return hasDupdName ? dedup(encoded, nameToIndex) : encoded;
  }

  /**
   * Batch encodes cookies into Set-Cookie header values.
   *
   * @param cookies a bunch of cookies
   *
   * @return the corresponding bunch of Set-Cookie headers
   */
  public List<String> encode(Iterable<? extends EnhancedCookie> cookies) {
    Iterator<? extends EnhancedCookie> cookiesIt = checkNotNull(cookies, "cookies").iterator();
    if (!cookiesIt.hasNext()) {
      return Collections.emptyList();
    }

    List<String> encoded = new ArrayList<String>();
    EnhancedCookie firstCookie = cookiesIt.next();
    Map<String, Integer> nameToIndex = strict && cookiesIt.hasNext() ? new HashMap<>() : null;
    int i = 0;
    encoded.add(encode(firstCookie));
    boolean hasDupdName = nameToIndex != null && nameToIndex.put(firstCookie.name(), i++) != null;
    while (cookiesIt.hasNext()) {
      EnhancedCookie c = cookiesIt.next();
      encoded.add(encode(c));
      if (nameToIndex != null) {
        hasDupdName |= nameToIndex.put(c.name(), i++) != null;
      }
    }
    return hasDupdName ? dedup(encoded, nameToIndex) : encoded;
  }
}
