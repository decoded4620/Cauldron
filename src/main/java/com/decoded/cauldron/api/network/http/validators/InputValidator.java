package com.decoded.cauldron.api.network.http.validators;

public interface InputValidator<T> {
  boolean validate(T value);
}
