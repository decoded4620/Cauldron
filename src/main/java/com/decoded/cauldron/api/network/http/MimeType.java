package com.decoded.cauldron.api.network.http;

public enum MimeType {
  APPLICATION_JSON("application/json"),
  MULTIPART_FORM_DATA("multipart/form-data"),
  TEXT_PLAIN("text/plain"),
  APPLICATION_JAVASCRIPT("application/javascript");

  private String mediaType;

  MimeType(String mediaType) {
    this.mediaType = mediaType;
  }

  public String getMediaType() {
    return mediaType;
  }

  @Override
  public String toString() {
    return mediaType;
  }
}
