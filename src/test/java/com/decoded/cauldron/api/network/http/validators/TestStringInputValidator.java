package com.decoded.cauldron.api.network.http.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validate a query parameter.
 */
public class TestStringInputValidator implements InputValidator<String> {
  private static final Logger LOG = LoggerFactory.getLogger(TestStringInputValidator.class);

  @Override
  public boolean validate(final String value) {
    if (value == null || value.equals("")) {
      LOG.error("Value: " + value + " was not valid, expected a non-empty, non-null value");
      return false;
    }

    LOG.info("Value: " + value + " was valid!");
    return true;
  }
}
