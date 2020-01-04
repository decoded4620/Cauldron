package com.decoded.cauldron.api.network.http;

public class EndpointResult {
  private Object result;
  private MimeType preferredMimeType = MimeType.TEXT_PLAIN;

  public EndpointResult(Object result, MimeType mimeType) {
    this.result = result;
    this.preferredMimeType = mimeType;
  }

  public Object getResult() {
    return result;
  }

  public MimeType getPreferredMimeType() {
    return preferredMimeType;
  }
}
