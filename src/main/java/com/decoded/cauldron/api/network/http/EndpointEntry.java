package com.decoded.cauldron.api.network.http;

import java.lang.reflect.Method;


public class EndpointEntry {
  private Method method;
  private MimeType responseMimeType;

  public EndpointEntry(Method method, MimeType mimeType) {
    this.method = method;
    this.responseMimeType = mimeType;
  }

  public Method getMethod() {
    return method;
  }

  public MimeType getResponseMimeType() {
    return responseMimeType;
  }
}
