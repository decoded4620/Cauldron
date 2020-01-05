package com.decoded.cauldron.api.network.security.crypto.google;

import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.google.common.annotations.VisibleForTesting;
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
 * This is the entry point for cryptographic operations in cauldron using Google Tink as the underlying service for cryptographic key management
 * and encryption / decryption operations.
 */
public class GoogleTinkCryptographyService implements CryptographyService {
  private static final Logger LOG = LoggerFactory.getLogger(GoogleTinkCryptographyService.class);
  private final String masterKeyUri;
  private final String cryptographicKeySetFile;
  private final String keysRelativeLocation;
  KeysetHandle keysetHandle;

  /**
   * Constructor.
   *
   * @param tinkCryptoConfig a {@link GoogleTinkConfiguration} instance to boot up the service.
   */
  public GoogleTinkCryptographyService(GoogleTinkConfiguration tinkCryptoConfig) {
    this.masterKeyUri = tinkCryptoConfig.getMasterKeyUri();
    this.cryptographicKeySetFile = tinkCryptoConfig.getCryptographicKeySetFile();
    this.keysRelativeLocation = tinkCryptoConfig.getKeysRelativeLocation();
  }

  @VisibleForTesting
  KeysetHandle getKeysetHandle() {
    return keysetHandle;
  }

  @Override
  public void initialize() {
    try {
      TinkConfig.register();
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error Registering internal Cryptography Implementation", ex);
    }
  }

  @Override
  public void loadEncryptionKeys() {
    String keySetFilename = keysRelativeLocation + '/' + cryptographicKeySetFile;
    File keySetFile = new File(keySetFilename);
    try {

      // The key set is encrypted with the this key in AWS KMS.
      keysetHandle = KeysetHandle.read(JsonKeysetReader.withFile(keySetFile), new AwsKmsClient().withDefaultCredentials().getAead(masterKeyUri));
      LOG.info("New key set Handle loaded: " + keysetHandle.getKeysetInfo().getPrimaryKeyId());
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error reading key set handle: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new CauldronServerException("Error reading key set file: " + keySetFile.getAbsolutePath() + ", " + ex.getMessage(), ex);
    }
  }

  @Override
  public void generateEncryptionKeys(boolean overwriteExisting) {
    try {
      KeyTemplate keyTemplate = AeadKeyTemplates.AES128_GCM;
      keysetHandle = KeysetHandle.generateNew(keyTemplate);

      String keySetFilename = keysRelativeLocation + '/' + cryptographicKeySetFile;
      File keySetFile = new File(keySetFilename);

      if (keySetFile.exists() && !overwriteExisting) {
        LOG.warn("Cannot generate key set, you must first remove the existing key set, or pass true for overwriteExisting parameter "
                + "to overwrite it here.");
      } else {
        LOG.info("New key set Handle generated: " + keysetHandle.getKeysetInfo().getPrimaryKeyId());
      }

      keysetHandle.write(JsonKeysetWriter.withFile(keySetFile), new AwsKmsClient().withDefaultCredentials().getAead(masterKeyUri));
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Error Generating Key set because: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new CauldronServerException("Error writing key set file", ex);
    }
  }

  @Override
  public byte[] encrypt(byte[] plainText, byte[] associatedData) {
    if (keysetHandle == null) {
      throw new IllegalStateException("You must load or generate a key set before you can encrypt information");
    }
    try {
      Aead aead = AeadFactory.getPrimitive(keysetHandle);
      return aead.encrypt(plainText, associatedData);
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Could not encrypt", ex);
    }
  }

  @Override
  public byte[] decrypt(byte[] cipherText, byte[] associatedData) {
    if (keysetHandle == null) {
      throw new IllegalStateException("You must load or generate a key set before you can decrypt information");
    }

    try {
      Aead aead = AeadFactory.getPrimitive(keysetHandle);
      return aead.decrypt(cipherText, associatedData);
    } catch (GeneralSecurityException ex) {
      throw new CauldronServerException("Could not decrypt", ex);
    }
  }
}
