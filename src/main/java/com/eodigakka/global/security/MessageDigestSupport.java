package com.eodigakka.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MessageDigestSupport {

  private MessageDigestSupport() {}

  public static boolean constantTimeEquals(String expected, String actual) {
    return MessageDigest.isEqual(bytes(expected), bytes(actual));
  }

  public static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(bytes(value));
      StringBuilder builder = new StringBuilder(hashed.length * 2);
      for (byte b : hashed) {
        builder.append(String.format("%02x", b & 0xff));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
    }
  }

  private static byte[] bytes(String value) {
    return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
  }
}
