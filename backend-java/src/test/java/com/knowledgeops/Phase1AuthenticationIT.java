package com.knowledgeops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.audit.infrastructure.AuditLogRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase1AuthenticationIT {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", "test-redis-password");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
    r.add("spring.datasource.username", MYSQL::getUsername);
    r.add("spring.datasource.password", MYSQL::getPassword);
    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    r.add("spring.data.redis.password", () -> "test-redis-password");
    r.add("knowledgeops.bootstrap.enabled", () -> "true");
    r.add("knowledgeops.bootstrap.email", () -> "admin@example.local");
    r.add("knowledgeops.bootstrap.password", () -> "AdminPassword!123");
    r.add("knowledgeops.bootstrap.display-name", () -> "Synthetic Admin");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AuditLogRepository audits;
  static String adminToken, userToken, userId, userRefresh, oldRefresh, userCsrf;
  static long userVersion;

  @Test
  @Order(1)
  void bootstrapAdminLoginMeAndRequestId() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .header("X-Request-ID", "phase1-smoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"admin@example.local\",\"password\":\"AdminPassword!123\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-ID", "phase1-smoke"))
            .andExpect(jsonPath("$.user.email").value("admin@example.local"))
            .andReturn();
    adminToken =
        json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0]").value("ADMINISTRATOR"));
  }

  @Test
  @Order(2)
  void adminCreatesUserAndDuplicateConflicts() throws Exception {
    String body =
        "{\"email\":\"user@example.local\",\"displayName\":\"Synthetic User\",\"initialPassword\":\"UserPassword!123\",\"roles\":[\"EMPLOYEE\"]}";
    MvcResult created =
        mvc.perform(
                post("/api/v1/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode createdUser = json.readTree(created.getResponse().getContentAsString());
    userId = createdUser.get("id").asText();
    userVersion = createdUser.get("version").asLong();
    mvc.perform(
            post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_CONFLICT"));
    MvcResult assigned =
        mvc.perform(
                put("/api/v1/users/" + userId + "/roles")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roles\":[\"EMPLOYEE\"],\"expectedVersion\":" + userVersion + "}"))
            .andExpect(status().isOk())
            .andReturn();
    userVersion =
        json.readTree(assigned.getResponse().getContentAsString()).get("version").asLong();
  }

  @Test
  @Order(3)
  void loginFailureAndValidationUseUnifiedErrors() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.local\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad\",\"password\":\"x\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @Order(4)
  void employeeAllowedMeButDeniedAdminAndRefreshRotates() throws Exception {
    MvcResult login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"user@example.local\",\"password\":\"UserPassword!123\"}"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode data = json.readTree(login.getResponse().getContentAsString());
    userToken = data.get("accessToken").asText();
    userCsrf = data.get("csrfToken").asText();
    userRefresh = cookie(login, "refresh_token");
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    oldRefresh = userRefresh;
    MvcResult rotated =
        mvc.perform(
                post("/api/v1/auth/refresh")
                    .cookie(
                        new jakarta.servlet.http.Cookie("refresh_token", userRefresh),
                        new jakarta.servlet.http.Cookie("csrf_token", userCsrf))
                    .header("X-CSRF-Token", userCsrf)
                    .header("Origin", "http://localhost:5173"))
            .andExpect(status().isOk())
            .andReturn();
    userRefresh = cookie(rotated, "refresh_token");
    userCsrf = json.readTree(rotated.getResponse().getContentAsString()).get("csrfToken").asText();
    assertThat(userRefresh).isNotEqualTo(oldRefresh);
  }

  @Test
  @Order(5)
  void logoutRevokesRefreshAndAccessSession() throws Exception {
    mvc.perform(
            post("/api/v1/auth/logout")
                .cookie(
                    new jakarta.servlet.http.Cookie("refresh_token", userRefresh),
                    new jakarta.servlet.http.Cookie("csrf_token", userCsrf))
                .header("X-CSRF-Token", userCsrf)
                .header("Origin", "http://localhost:5173"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isUnauthorized());
    for (String token : List.of(oldRefresh, userRefresh))
      mvc.perform(
              post("/api/v1/auth/refresh")
                  .cookie(
                      new jakarta.servlet.http.Cookie("refresh_token", token),
                      new jakarta.servlet.http.Cookie("csrf_token", userCsrf))
                  .header("X-CSRF-Token", userCsrf)
                  .header("Origin", "http://localhost:5173"))
          .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(6)
  void disabledUserCannotLoginAndAuditExists() throws Exception {
    mvc.perform(
            patch("/api/v1/users/" + userId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\",\"expectedVersion\":" + userVersion + "}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.local\",\"password\":\"UserPassword!123\"}"))
        .andExpect(status().isUnauthorized());
    assertThat(audits.findAll().stream().map(x -> x.getAction()))
        .contains(
            AuditAction.LOGIN_SUCCESS,
            AuditAction.LOGIN_FAILURE,
            AuditAction.REFRESH,
            AuditAction.LOGOUT,
            AuditAction.ROLE_ASSIGN,
            AuditAction.USER_DISABLE);
  }

  private static String cookie(MvcResult result, String name) {
    return result.getResponse().getHeaders("Set-Cookie").stream()
        .filter(x -> x.startsWith(name + "="))
        .map(x -> x.substring(name.length() + 1, x.indexOf(';')))
        .findFirst()
        .orElseThrow();
  }
}
