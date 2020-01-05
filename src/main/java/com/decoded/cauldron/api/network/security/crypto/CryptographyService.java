package com.decoded.cauldron.api.network.security.crypto;

public interface CryptographyService {
  /**
   * Initializes the service. The underlying implementation performs its setup in this block.
   */
  void initialize();

  /**
   * Loads encryption keys that were previously generated. If no keys were generated, this will throw an exception.
   */
  void loadEncryptionKeys();

  /**
   * Generates encryption keys. This requires a master key uri setup with either Google or Aws KMS.
   *
   * @param overwriteExisting true to stomp existing keys.
   */
  void generateEncryptionKeys(boolean overwriteExisting);

  /**
   * Encrypt Plaintext with associated data.
   *
   * @param plainText      the plain text bytes
   * @param associatedData the associated data
   *
   * @return encrypted cipher
   */
  byte[] encrypt(byte[] plainText, byte[] associatedData);

  /**
   * Decrypt cipher text.
   *
   * @param cipherText     the encrypted cipher
   * @param associatedData the associated bytes
   *
   * @return the decrypted bytes from the cipher.
   */
  byte[] decrypt(byte[] cipherText, byte[] associatedData);
}
