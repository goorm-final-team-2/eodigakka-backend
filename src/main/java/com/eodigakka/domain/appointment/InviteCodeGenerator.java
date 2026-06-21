package com.eodigakka.domain.appointment;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class InviteCodeGenerator {

  private static final char[] CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int CODE_LENGTH = 8;

  private final SecureRandom secureRandom = new SecureRandom();

  public String generate() {
    StringBuilder builder = new StringBuilder(CODE_LENGTH);
    for (int index = 0; index < CODE_LENGTH; index++) {
      builder.append(CHARACTERS[secureRandom.nextInt(CHARACTERS.length)]);
    }
    return builder.toString();
  }
}
