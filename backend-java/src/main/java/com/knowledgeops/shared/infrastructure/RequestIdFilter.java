package com.knowledgeops.shared.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
  public static final String ATTRIBUTE = "knowledgeops.requestId";
  private static final Pattern SAFE = Pattern.compile("(?:[A-Za-z0-9_-]{1,64})");
  private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String supplied = req.getHeader("X-Request-ID");
    String id =
        supplied != null && SAFE.matcher(supplied).matches()
            ? supplied
            : UUID.randomUUID().toString();
    long start = System.nanoTime();
    req.setAttribute(ATTRIBUTE, id);
    res.setHeader("X-Request-ID", id);
    MDC.put("requestId", id);
    try {
      chain.doFilter(req, res);
    } finally {
      log.info(
          "http_request method={} path={} status={} durationMs={}",
          req.getMethod(),
          req.getRequestURI(),
          res.getStatus(),
          (System.nanoTime() - start) / 1_000_000);
      MDC.remove("requestId");
      MDC.remove("userId");
    }
  }
}
