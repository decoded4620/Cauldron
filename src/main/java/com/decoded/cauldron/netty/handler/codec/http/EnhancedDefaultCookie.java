package com.decoded.cauldron.netty.handler.codec.http;

import com.decoded.cauldron.server.http.cookies.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Extension of Netty's default cookie which will include Same Site and Expires support.
 */
public class EnhancedDefaultCookie extends DefaultCookie implements EnhancedCookie {

  public SameSite sameSite;

  public EnhancedDefaultCookie(String name, String value) {
    super(name, value);
  }

  @Override
  public SameSite getSameSite() {
    return sameSite;
  }

  public EnhancedDefaultCookie setSameSite(final SameSite sameSite) {
    this.sameSite = sameSite;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final EnhancedDefaultCookie that = (EnhancedDefaultCookie) o;

    return new EqualsBuilder().appendSuper(super.equals(o)).append(sameSite, that.sameSite).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(sameSite).toHashCode();
  }
}
