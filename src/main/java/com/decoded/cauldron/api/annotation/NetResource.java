package com.decoded.cauldron.api.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Resource Annotation to expose a class as a Network Resource in a Cauldron Http Container.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NetResource {
  /**
   * The route for this resource, e.g. <code>/myUrl</code>
   *
   * @return a String
   */
  String route();
}
