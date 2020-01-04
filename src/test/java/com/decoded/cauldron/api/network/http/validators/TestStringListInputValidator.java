package com.decoded.cauldron.api.network.http.validators;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for List input.
 */
public class TestStringListInputValidator implements InputValidator<List<String>> {
  private static final Logger LOG = LoggerFactory.getLogger(TestStringListInputValidator.class);

  @Override
  public boolean validate(final List<String> value) {
    if (value.isEmpty()) {
      LOG.error("String array input is empty");
      return false;
    }

    return true;
  }
}
