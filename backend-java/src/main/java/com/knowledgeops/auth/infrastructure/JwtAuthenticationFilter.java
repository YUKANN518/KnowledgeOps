package com.knowledgeops.auth.infrastructure;

import com.knowledgeops.auth.application.*;
import com.knowledgeops.shared.domain.BusinessException;
import com.knowledgeops.user.domain.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final UserRepository users;
  private final SessionRegistry sessions;
  private final SecurityErrorWriter errors;

  public JwtAuthenticationFilter(
      JwtService jwt, UserRepository users, SessionRegistry sessions, SecurityErrorWriter errors) {
    this.jwt = jwt;
    this.users = users;
    this.sessions = sessions;
    this.errors = errors;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(req, res);
      return;
    }
    try {
      var decoded = jwt.decode(header.substring(7));
      UUID userId = UUID.fromString(decoded.getSubject());
      UUID sid = UUID.fromString(decoded.getClaimAsString("sid"));
      User user = users.findById(userId).orElseThrow();
      if (user.getStatus() != UserStatus.ACTIVE || !sessions.isActive(sid, userId))
        throw new JwtException("inactive session");
      Set<String> roles = new HashSet<>();
      Set<String> permissions = new HashSet<>();
      user.getRoles()
          .forEach(
              r -> {
                roles.add(r.getCode().name());
                r.getPermissions().forEach(p -> permissions.add(p.getCode()));
              });
      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
      permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
      var principal =
          new AuthenticatedUser(
              userId, user.getEmail(), Set.copyOf(roles), Set.copyOf(permissions));
      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
      MDC.put("userId", userId.toString());
      chain.doFilter(req, res);
    } catch (BusinessException ex) {
      errors.write(req, res, ex.getStatus(), ex.getCode().name(), ex.getMessage());
    } catch (JwtException | IllegalArgumentException | NoSuchElementException ex) {
      errors.write(
          req, res, HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "Access token is invalid or revoked");
    }
  }
}
