package com.decoded.cauldron.api.config;

/**
 * Config Scope is define scopes for Typesafe Hocon configuration format, and allow configs to be "scoped" by their path to configuration.
 */
public class ConfigScope {
  static final Character PATH_SEPARATOR = '.';

  private String path = "";

  /**
   * Constructor.
   */
  public ConfigScope() {
  }

  public ConfigScope(String name) {
    this.path = name;
  }

  public String getPath() {
    return path;
  }

  /**
   * Create a child {@link ConfigScope}.
   *
   * @param childPath path to child config value.
   *
   * @return a {@link ConfigScope} that is a child of this {@link ConfigScope}
   */
  public ConfigScope child(String childPath) {
    return new ConfigScope(getPath().isEmpty() ? childPath : getPath() + PATH_SEPARATOR + childPath);
  }
}
