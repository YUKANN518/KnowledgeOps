package com.knowledgeops.auth.infrastructure;

import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final SecurityErrorWriter writer;

  public RestAuthenticationEntryPoint(SecurityErrorWriter writer) {
    this.writer = writer;
  }

  public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
      throws IOException {
    writer.write(
        req, res, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required");
  }
}
