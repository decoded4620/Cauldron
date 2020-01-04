package com.decoded.cauldron.api.network.http.validators;

public class NoopInputValidator implements InputValidator {
  @Override
  public boolean validate(final Object value) {
    return true;
  }
}
