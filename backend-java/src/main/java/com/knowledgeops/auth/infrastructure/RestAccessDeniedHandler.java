package com.knowledgeops.auth.infrastructure;

import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
  private final SecurityErrorWriter writer;

  public RestAccessDeniedHandler(SecurityErrorWriter writer) {
    this.writer = writer;
  }

  public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
      throws IOException {
    writer.write(req, res, HttpStatus.FORBIDDEN, "FORBIDDEN", "Operation is not permitted");
  }
}
