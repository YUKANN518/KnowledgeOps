package com.knowledgeops.auth.infrastructure;

import com.knowledgeops.shared.api.ErrorResponse;
import com.knowledgeops.shared.infrastructure.RequestIdFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorWriter {
  private final ObjectMapper mapper;

  public SecurityErrorWriter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public void write(
      HttpServletRequest req,
      HttpServletResponse res,
      HttpStatus status,
      String code,
      String message)
      throws IOException {
    if (res.isCommitted()) return;
    res.setStatus(status.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Object id = req.getAttribute(RequestIdFilter.ATTRIBUTE);
    mapper.writeValue(
        res.getOutputStream(),
        new ErrorResponse(code, message, List.of(), String.valueOf(id), Instant.now()));
  }
}
