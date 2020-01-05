package com.decoded.cauldron.api.network.security.crypto.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

import com.decoded.cauldron.server.exception.CauldronServerException;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CryptographyServiceTest {
  private static final String UTF_8 = "UTF-8";
  private static GoogleTinkCryptographyService cryptographyService;

  @BeforeAll
  static void setup() {
    // The arn is available through your key management service. Set it up in environment variables.
    final String masterKeyUri = "aws-kms://" + System.getenv("DEV_MASTER_KEY_ARN");
    final String cryptographicKeySetFile = "cauldron_key_set_test.json";
    final String keysRelativeLocation = "keys";

    cryptographyService = spy(
        new GoogleTinkCryptographyService(new GoogleTinkConfiguration(masterKeyUri, cryptographicKeySetFile, keysRelativeLocation)));

    cryptographyService.initialize();
    cryptographyService.generateEncryptionKeys(false);
  }

  @Test
  public void testRegenerateKeys() {
    // this should not fail
    cryptographyService.generateEncryptionKeys(true);
    assertNotNull(cryptographyService.getKeysetHandle());
  }

  @Test
  public void testLoadKeys() {
    // this should not fail
    cryptographyService.loadEncryptionKeys();
    assertNotNull(cryptographyService.getKeysetHandle());
  }

  @Test
  public void testEncryptDecrypt() {
    try {
      String original = "Test";
      byte[] originalBytes = original.getBytes(UTF_8);
      byte[] assocData = "x".getBytes(UTF_8);
      byte[] encryptedBytes = cryptographyService.encrypt(originalBytes, assocData);
      String encrypted = new String(encryptedBytes, UTF_8);
      byte[] decryptedBytes = cryptographyService.decrypt(encryptedBytes, assocData);
      String decrypted = new String(decryptedBytes, UTF_8);

      assertNotEquals(decrypted, encrypted);
      assertEquals(original, decrypted);

    } catch (UnsupportedEncodingException ex) {
      throw new CauldronServerException("Bad encoding", ex);
    }
  }
}
