package com.decoded.cauldron.api.annotation;

import com.decoded.cauldron.api.network.http.validators.InputValidator;
import com.decoded.cauldron.api.network.http.validators.NoopInputValidator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares that input for a formal parameter declaration is to be supplied by a query parameter during invocation of a Resources endpoint.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {
  /**
   * The Parameter Name.
   *
   * @return a String
   */
  String name();

  /**
   * Flag for optionally omitting this parameter from the request body.
   *
   * @return a boolean
   */
  boolean optional() default false;

  /**
   * The validator used to validate the parameter value.
   *
   * @return a Class which is an instance of {@link InputValidator}
   */
  Class<? extends InputValidator> validator() default NoopInputValidator.class;
}
