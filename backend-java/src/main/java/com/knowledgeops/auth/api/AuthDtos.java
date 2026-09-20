package com.knowledgeops.auth.api;

import com.knowledgeops.user.api.UserDtos;
import jakarta.validation.constraints.*;

public final class AuthDtos {
  private AuthDtos() {}

  public record Login(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

  public record Tokens(
      String accessToken, long expiresIn, String csrfToken, UserDtos.Response user) {}
}
