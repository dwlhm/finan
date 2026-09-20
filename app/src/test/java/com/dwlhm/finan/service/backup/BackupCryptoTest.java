package com.dwlhm.finan.service.backup;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.Test;

public class BackupCryptoTest {
  private static final char[] PASSWORD = "correct horse battery staple".toCharArray();
  private static final byte[] DATA = "complete financial snapshot 🪙".getBytes(StandardCharsets.UTF_8);

  @Test public void roundTripAndEveryExportUsesFreshRandomness() throws Exception {
    byte[] first = BackupCrypto.encrypt(DATA, PASSWORD);
    byte[] second = BackupCrypto.encrypt(DATA, PASSWORD);
    assertArrayEquals(DATA, BackupCrypto.decrypt(first, PASSWORD));
    assertArrayEquals(DATA, BackupCrypto.decrypt(second, PASSWORD));
    assertFalse(Arrays.equals(Arrays.copyOfRange(first, 16, 32), Arrays.copyOfRange(second, 16, 32)));
    assertFalse(Arrays.equals(Arrays.copyOfRange(first, 32, 44), Arrays.copyOfRange(second, 32, 44)));
    assertFalse(new String(first, StandardCharsets.UTF_8).contains("complete financial"));
  }
  @Test public void wrongPasswordAndTamperFailAuthentication() throws Exception {
    byte[] encrypted = BackupCrypto.encrypt(DATA, PASSWORD);
    assertThrows(GeneralSecurityException.class, () -> BackupCrypto.decrypt(encrypted, "wrong".toCharArray()));
    for (int offset : new int[] {16, 32, 44, encrypted.length - 1}) {
      byte[] modified = encrypted.clone(); modified[offset] ^= 1;
      assertThrows(GeneralSecurityException.class, () -> BackupCrypto.decrypt(modified, PASSWORD));
    }
  }
  @Test public void truncationUnsupportedVersionAndKdfAreRejected() throws Exception {
    byte[] encrypted = BackupCrypto.encrypt(DATA, PASSWORD);
    assertThrows(IOException.class, () -> BackupCrypto.decrypt(Arrays.copyOf(encrypted, 43), PASSWORD));
    assertThrows(GeneralSecurityException.class, () -> BackupCrypto.decrypt(Arrays.copyOf(encrypted, encrypted.length - 1), PASSWORD));
    for (int offset : new int[] {0, 11, 15}) {
      byte[] modified = encrypted.clone(); modified[offset] ^= 1;
      assertThrows(IOException.class, () -> BackupCrypto.decrypt(modified, PASSWORD));
    }
  }
  @Test public void payloadLimitAndEmptyPasswordRejected() {
    assertThrows(IOException.class, () -> BackupCrypto.encrypt(new byte[BackupCrypto.MAX_FILE_BYTES], PASSWORD));
    assertThrows(IOException.class, () -> BackupCrypto.decrypt(new byte[BackupCrypto.MAX_FILE_BYTES + 1], PASSWORD));
    assertThrows(GeneralSecurityException.class, () -> BackupCrypto.encrypt(DATA, new char[0]));
  }
}
