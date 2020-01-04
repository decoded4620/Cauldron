package com.decoded.cauldron.api.annotation;

import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.MimeType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations for methods in a Network Resource.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpEndpoint {
  /**
   * The method for the endpoint.
   *
   * @return CauldronHttpMethod
   */
  CauldronHttpMethod method() default CauldronHttpMethod.GET;

  /**
   * The mime type for the response if there is content being returned.
   *
   * @return a MimeType
   */
  MimeType responseMimeType() default MimeType.APPLICATION_JSON;
}
