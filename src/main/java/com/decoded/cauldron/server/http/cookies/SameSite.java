package com.decoded.cauldron.server.http.cookies;

public enum SameSite {
  LAX("Lax"), STRICT("Strict"), NONE("None");

  private String value;

  SameSite(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
