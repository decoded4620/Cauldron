package com.decoded.cauldron.netty.handler.codec.http;

import com.decoded.cauldron.server.http.cookies.SameSite;
import io.netty.handler.codec.http.cookie.Cookie;

public interface EnhancedCookie extends Cookie {
  /**
   * Same Site Support.
   *
   * @return SameSite
   */
  SameSite getSameSite();
}
