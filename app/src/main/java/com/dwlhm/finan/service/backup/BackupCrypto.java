package com.dwlhm.finan.service.backup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Portable authenticated envelope limited to 32 MiB; no key or password is persisted.
 * Uses the platform JCA/JCE SecretKeyFactory PBKDF2WithHmacSHA256 and Cipher AES/GCM/NoPadding,
 * with random salts/nonces from SecureRandom and an authenticated versioned header.
 */
public final class BackupCrypto {
  public static final int MAX_FILE_BYTES = 32 * 1024 * 1024;
  private static final byte[] MAGIC = "FINANBAK".getBytes(StandardCharsets.US_ASCII);
  private static final int ITERATIONS = 600_000;
  private static final int HEADER_BYTES = 8 + 4 + 4 + 16 + 12;
  private BackupCrypto() {}

  /** Encrypts with a fresh salt and nonce; the caller retains ownership of plaintext and password. */
  public static byte[] encrypt(byte[] plaintext, char[] password)
      throws GeneralSecurityException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    encrypt(plaintext, password, buffer);
    return buffer.toByteArray();
  }

  /** Streams the same envelope format as {@link #encrypt(byte[], char[])} without buffering the ciphertext. */
  public static void encrypt(byte[] plaintext, char[] password, OutputStream output)
      throws GeneralSecurityException, IOException {
    if (output == null || plaintext == null || plaintext.length > MAX_FILE_BYTES - HEADER_BYTES - 16)
      throw new java.io.StreamCorruptedException("Backup exceeds 32 MiB limit");
    if (password == null || password.length == 0) throw new GeneralSecurityException("Password required");
    byte[] salt = new byte[16], nonce = new byte[12];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt); random.nextBytes(nonce);
    byte[] header = ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).putInt(1)
        .putInt(ITERATIONS).put(salt).put(nonce).array();
    PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
    byte[] key = null;
    try {
      key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      cipher.updateAAD(header);
      output.write(header);
      if (plaintext.length > 0) {
        byte[] chunk = cipher.update(plaintext);
        if (chunk != null) output.write(chunk);
      }
      output.write(cipher.doFinal());
    } finally {
      spec.clearPassword();
      if (key != null) Arrays.fill(key, (byte) 0);
    }
  }

  /** Authenticates the entire envelope before returning plaintext; rejects unsupported headers. */
  public static byte[] decrypt(byte[] envelope, char[] password)
      throws GeneralSecurityException, IOException {
    if (envelope == null || envelope.length < HEADER_BYTES + 16 || envelope.length > MAX_FILE_BYTES)
      throw new java.io.StreamCorruptedException("Invalid backup size");
    ByteBuffer input = ByteBuffer.wrap(envelope);
    byte[] magic = new byte[8]; input.get(magic);
    if (!Arrays.equals(magic, MAGIC) || input.getInt() != 1 || input.getInt() != ITERATIONS)
      throw new java.io.StreamCorruptedException("Unsupported backup format; update the app");
    byte[] salt = new byte[16], nonce = new byte[12]; input.get(salt); input.get(nonce);
    return crypt(Cipher.DECRYPT_MODE, Arrays.copyOfRange(envelope, HEADER_BYTES, envelope.length),
        password, salt, nonce, Arrays.copyOf(envelope, HEADER_BYTES));
  }

  private static byte[] crypt(int mode, byte[] input, char[] password, byte[] salt,
      byte[] nonce, byte[] header) throws GeneralSecurityException {
    if (password == null || password.length == 0) throw new GeneralSecurityException("Password required");
    PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
    byte[] key = null;
    try {
      key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      cipher.updateAAD(header);
      return cipher.doFinal(input);
    } finally {
      spec.clearPassword();
      if (key != null) Arrays.fill(key, (byte) 0);
    }
  }
}
