package com.decoded.cauldron.api.network;

/**
 * Abstract network resource implementation.
 */
public abstract class AbstractNetworkResource {
  private String path;

  public String getPath() {
    return path;
  }

  public AbstractNetworkResource setPath(final String path) {
    this.path = path;
    return this;
  }
}
