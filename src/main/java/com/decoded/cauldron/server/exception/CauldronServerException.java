package com.decoded.cauldron.server.exception;

public class CauldronServerException extends RuntimeException {
  public CauldronServerException(String message) {
    super(message);
  }

  public CauldronServerException(Throwable cause) {
    super(cause);
  }

  public CauldronServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
