package com.knowledgeops.auth.application;

import com.knowledgeops.auth.infrastructure.AuthProperties;
import com.knowledgeops.shared.domain.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
public class RefreshCsrfService {
  public static final String COOKIE = "csrf_token";
  private final AuthProperties props;

  public RefreshCsrfService(AuthProperties props) {
    this.props = props;
  }

  public void validate(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, COOKIE);
    String header = request.getHeader("X-CSRF-Token");
    String origin = request.getHeader("Origin");
    boolean same =
        cookie != null
            && header != null
            && MessageDigest.isEqual(
                cookie.getValue().getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8));
    if (!same || !props.allowedOrigin().equals(origin))
      throw new BusinessException(
          ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "CSRF validation failed");
  }
}
