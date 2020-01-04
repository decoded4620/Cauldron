package com.decoded.cauldron.server.exception;

import com.decoded.cauldron.server.http.Status;


public class CauldronHttpException extends RuntimeException {
  private Status responseStatus;

  public CauldronHttpException(Status status, String message) {
    super(message);
    this.responseStatus = status;
  }

  public CauldronHttpException(Status status, Throwable cause) {
    super(cause);
    this.responseStatus = status;
  }

  public CauldronHttpException(Status status, String message, Throwable cause) {
    super(message, cause);
    this.responseStatus = status;
  }

  public Status getResponseStatus() {
    return responseStatus;
  }
}
