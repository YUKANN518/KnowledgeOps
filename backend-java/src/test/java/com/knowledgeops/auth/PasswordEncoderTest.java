package com.knowledgeops.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {
  @Test
  void hashesPasswordsWithSaltAndNeverStoresPlainText() {
    var encoder = new BCryptPasswordEncoder(12);
    String one = encoder.encode("SyntheticPassword!123");
    String two = encoder.encode("SyntheticPassword!123");
    assertThat(one).doesNotContain("SyntheticPassword").isNotEqualTo(two);
    assertThat(encoder.matches("SyntheticPassword!123", one)).isTrue();
  }
}
