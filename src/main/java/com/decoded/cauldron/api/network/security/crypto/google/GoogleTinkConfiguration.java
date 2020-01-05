package com.decoded.cauldron.api.network.security.crypto.google;

/**
 * Cryptography Service Configuration for Cauldron Cryptography Service implementation backed by Google Tink.
 */
public class GoogleTinkConfiguration {
  private final String masterKeyUri;
  private final String cryptographicKeySetFile;
  private final String keysRelativeLocation;

  /**
   * Ctor.
   *
   * @param masterKeyUri            the master key uri
   * @param cryptographicKeySetFile the key set file name
   * @param keysRelativeLocation    the relative location to store key sets.
   */
  public GoogleTinkConfiguration(String masterKeyUri, String cryptographicKeySetFile, String keysRelativeLocation) {
    this.masterKeyUri = masterKeyUri;
    this.cryptographicKeySetFile = cryptographicKeySetFile;
    this.keysRelativeLocation = keysRelativeLocation;
  }

  /**
   * The master key set file name.
   *
   * @return String
   */
  public String getCryptographicKeySetFile() {
    return cryptographicKeySetFile;
  }

  /**
   * The relative key set file location directory.
   *
   * @return a String
   */
  public String getKeysRelativeLocation() {
    return keysRelativeLocation;
  }

  /**
   * The master key URI, e.g. from Google cloud or Amazon KMS which is used to load the Customer Master Key which is used to encrypt / decrypt
   * user data.
   *
   * @return String
   */
  public String getMasterKeyUri() {
    return masterKeyUri;
  }
}
