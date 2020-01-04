package com.decoded.cauldron.api.network.security;

import com.decoded.cauldron.server.exception.CauldronServerException;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.integration.awskms.AwsKmsClient;
import com.google.crypto.tink.proto.KeyTemplate;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the entry point for cryptographic operations in cauldron.
 */
public class CryptographyService {
  private static final Logger LOG = LoggerFactory.getLogger(CryptographyService.class);

  private KeysetHandle keysetHandle;
  private String masterKeyUri = "aws-kms://arn:aws:kms:us-east-2:914607887564:key/522aa4ea-4ee0-4937-96c0-84dac1534e14";

  /**
   * Initializes the service.
   */
  public void initialize() {
    try {
      TinkConfig.register();
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error Registering internal Cryptography Implementation", ex);
    }
  }

  /**
   * Loads encryption keys.
   *
   * @param fromRelativeLocation load from the relative directory under the program directory.
   */
  public void loadEncryptionKeys(String fromRelativeLocation) {
    String keySetFilename = fromRelativeLocation + "/cauldron_key_set.json";
    File keySetFile = new File(keySetFilename);
    try {

      // The key set is encrypted with the this key in AWS KMS.
      keysetHandle = KeysetHandle.read(JsonKeysetReader.withFile(keySetFile), new AwsKmsClient().withDefaultCredentials().getAead(masterKeyUri));
      LOG.info("New keysetHandle loaded: " + keysetHandle.getKeysetInfo().getPrimaryKeyId());
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error reading key set handle", ex);
    } catch (IOException ex) {
      throw new CauldronServerException("Error reading key set file: " + keySetFile.getAbsolutePath());
    }
  }

  /**
   * Generates encryption keys.
   *
   * @param toRelativeLocation to the relative location from the program directory
   * @param overwriteExisting  true to stomp existing keys.
   */
  public void generateEncryptionKeys(String toRelativeLocation, boolean overwriteExisting) {
    try {
      KeyTemplate keyTemplate = AeadKeyTemplates.AES128_GCM;
      keysetHandle = KeysetHandle.generateNew(keyTemplate);

      String keySetFilename = toRelativeLocation + "/cauldron_key_set.json";
      File keySetFile = new File(keySetFilename);

      if (keySetFile.exists() && !overwriteExisting) {
        LOG.warn("Cannot generate key set, you must first remove the existing key set, "
            + "or pass true for overwriteExisting parameter to overwrite it here.");
      } else {
        LOG.info("New keysetHandle generated: " + keysetHandle.getKeysetInfo().getPrimaryKeyId());
      }

      keysetHandle.write(JsonKeysetWriter.withFile(keySetFile), new AwsKmsClient().withDefaultCredentials().getAead(masterKeyUri));
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error Generating Key set", ex.getCause());
    } catch (IOException ex) {
      throw new CauldronServerException("Error writing key set file", ex);
    }
  }

  /**
   * Encrypt Plaintext with associated data.
   *
   * @param plainText      the plain text bytes
   * @param associatedData the associated data
   *
   * @return encrypted cipher
   */
  public byte[] encrypt(byte[] plainText, byte[] associatedData) {
    try {
      Aead aead = AeadFactory.getPrimitive(keysetHandle);
      return aead.encrypt(plainText, associatedData);
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Could not encrypt", ex);
    }
  }

  /**
   * Decrypt cipher text.
   *
   * @param cipherText the encrypted cipher
   * @param associatedData the associated bytes
   *
   * @return the decrypted bytes from the cipher.
   */
  public byte[] decrypt(byte[] cipherText, byte[] associatedData) {
    try {
      Aead aead = AeadFactory.getPrimitive(keysetHandle);
      return aead.decrypt(cipherText, associatedData);
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Could not decrypt", ex);
    }
  }
}
