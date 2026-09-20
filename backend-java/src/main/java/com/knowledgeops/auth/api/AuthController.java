package com.knowledgeops.auth.api;

import com.knowledgeops.auth.application.*;
import com.knowledgeops.auth.infrastructure.AuthProperties;
import com.knowledgeops.shared.domain.*;
import com.knowledgeops.user.api.UserDtos;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  public static final String REFRESH_COOKIE = "refresh_token";
  private final AuthService auth;
  private final RefreshCsrfService csrf;
  private final AuthProperties props;

  public AuthController(AuthService auth, RefreshCsrfService csrf, AuthProperties props) {
    this.auth = auth;
    this.csrf = csrf;
    this.props = props;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthDtos.Tokens> login(@Valid @RequestBody AuthDtos.Login input) {
    return response(auth.login(input.email(), input.password()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthDtos.Tokens> refresh(HttpServletRequest request) {
    csrf.validate(request);
    var cookie = WebUtils.getCookie(request, REFRESH_COOKIE);
    if (cookie == null) throw unauthenticated();
    return response(auth.refresh(cookie.getValue()));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      HttpServletRequest request,
      HttpServletResponse response,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    csrf.validate(request);
    var refresh = WebUtils.getCookie(request, REFRESH_COOKIE);
    if (refresh != null) auth.logout(refresh.getValue(), actor == null ? null : actor.id());
    response.addHeader(
        HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", Duration.ZERO, true).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        cookie(RefreshCsrfService.COOKIE, "", Duration.ZERO, false).toString());
  }

  private ResponseEntity<AuthDtos.Tokens> response(AuthService.Tokens tokens) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        cookie(REFRESH_COOKIE, tokens.refreshToken(), props.refreshTokenTtl(), true).toString());
    headers.add(
        HttpHeaders.SET_COOKIE,
        cookie(RefreshCsrfService.COOKIE, tokens.csrfToken(), props.refreshTokenTtl(), false)
            .toString());
    return new ResponseEntity<>(
        new AuthDtos.Tokens(
            tokens.accessToken(),
            tokens.expiresIn(),
            tokens.csrfToken(),
            UserDtos.Response.from(tokens.user())),
        headers,
        HttpStatus.OK);
  }

  private ResponseCookie cookie(String name, String value, Duration maxAge, boolean httpOnly) {
    return ResponseCookie.from(name, value)
        .httpOnly(httpOnly)
        .secure(props.refreshCookieSecure())
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(maxAge)
        .build();
  }

  private BusinessException unauthenticated() {
    return new BusinessException(
        ErrorCode.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED, "Refresh token is required");
  }
}
