package com.knowledgeops.auth.infrastructure;

import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.user.application.UserService;
import com.knowledgeops.user.domain.*;
import java.util.Set;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "knowledgeops.bootstrap.enabled", havingValue = "true")
public class BootstrapAdminRunner implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);
  private final UserRepository users;
  private final UserService service;
  private final String email;
  private final String password;
  private final String name;

  public BootstrapAdminRunner(
      UserRepository users,
      UserService service,
      @Value("${knowledgeops.bootstrap.email}") String email,
      @Value("${knowledgeops.bootstrap.password}") String password,
      @Value("${knowledgeops.bootstrap.display-name}") String name) {
    this.users = users;
    this.service = service;
    this.email = email;
    this.password = password;
    this.name = name;
  }

  public void run(String... args) {
    if (email.isBlank() || password.isBlank())
      throw new IllegalStateException("Bootstrap admin email/password are required when enabled");
    if (users.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).isPresent()) {
      log.info("bootstrap_admin_exists");
      return;
    }
    service.create(
        email, name, password, Set.of(RoleCode.ADMINISTRATOR), null, AuditAction.BOOTSTRAP_ADMIN);
    log.info("bootstrap_admin_created");
  }
}
