package com.knowledgeops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.knowledgeops.audit.domain.*;
import com.knowledgeops.audit.infrastructure.AuditLogRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase2TicketIT {
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
  @Autowired JdbcTemplate jdbc;

  static String adminToken;
  static String employeeToken;
  static String otherEmployeeToken;
  static String supportToken;
  static String employeeId;
  static String otherEmployeeId;
  static String supportId;
  static String ticketId;
  static long ticketVersion;

  @Test
  @Order(1)
  void bootstrapUsersAndV2MigrationIsApplied() throws Exception {
    adminToken = login("admin@example.local", "AdminPassword!123");
    employeeId =
        createUser(
            "phase2.employee@example.local",
            "Phase Two Employee",
            "EmployeePassword!123",
            "EMPLOYEE");
    otherEmployeeId =
        createUser("phase2.other@example.local", "Other Employee", "OtherPassword!123", "EMPLOYEE");
    supportId =
        createUser(
            "phase2.support@example.local",
            "Support Agent",
            "SupportPassword!123",
            "SUPPORT_AGENT");
    employeeToken = login("phase2.employee@example.local", "EmployeePassword!123");
    otherEmployeeToken = login("phase2.other@example.local", "OtherPassword!123");
    supportToken = login("phase2.support@example.local", "SupportPassword!123");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version='2' and success=1",
                Integer.class))
        .isEqualTo(1);
    assertThat(tableCount("tickets")).isEqualTo(1);
    assertThat(tableCount("ticket_comments")).isEqualTo(1);
    assertThat(primaryKeyCount("tickets")).isEqualTo(1);
    assertThat(indexCount("tickets", "ix_tickets_assignee")).isGreaterThan(0);
    assertThat(foreignKeyCount("ticket_comments", "fk_ticket_comments_ticket")).isEqualTo(1);
  }

  @Test
  @Order(2)
  void employeeCreatesReadsAndCannotCrossTicketBoundary() throws Exception {
    mvc.perform(get("/api/v1/tickets"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    MvcResult created =
        mvc.perform(
                post("/api/v1/tickets")
                    .header("Authorization", bearer(employeeToken))
                    .header("X-Request-ID", "phase2-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Cannot access payroll portal","description":"The portal rejects my valid account.","priority":"HIGH"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.creatorId").value(employeeId))
            .andReturn();
    JsonNode ticket = body(created);
    ticketId = ticket.get("id").asText();
    ticketVersion = ticket.get("version").asLong();

    mvc.perform(get("/api/v1/tickets/" + ticketId).header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("The portal rejects my valid account."));
    mvc.perform(
            get("/api/v1/tickets/" + ticketId).header("Authorization", bearer(otherEmployeeToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TICKET_ACCESS_DENIED"));
    mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/assignments")
                .header("Authorization", bearer(employeeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":\"" + supportId + "\",\"expectedVersion\":0}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @Order(3)
  void listUsesAclFiltersPaginationAndMaximumSize() throws Exception {
    mvc.perform(
            post("/api/v1/tickets")
                .header("Authorization", bearer(employeeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Second payroll issue","description":"Used to verify pagination.","priority":"HIGH"}
                    """))
        .andExpect(status().isCreated());

    mvc.perform(
            get("/api/v1/tickets")
                .queryParam("priority", "HIGH")
                .queryParam("status", "OPEN")
                .queryParam("page", "0")
                .queryParam("size", "1")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));

    mvc.perform(get("/api/v1/tickets").header("Authorization", bearer(supportToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));

    mvc.perform(get("/api/v1/tickets/" + ticketId).header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk());

    mvc.perform(
            get("/api/v1/tickets")
                .queryParam("size", "101")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @Order(4)
  void supportReadsAssignsAndRejectsInvalidOrStaleAssignment() throws Exception {
    mvc.perform(get("/api/v1/tickets/" + ticketId).header("Authorization", bearer(supportToken)))
        .andExpect(status().isOk());

    mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/assignments")
                .header("Authorization", bearer(supportToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"assigneeId\":\""
                        + otherEmployeeId
                        + "\",\"expectedVersion\":"
                        + ticketVersion
                        + "}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));

    long staleVersion = ticketVersion;
    MvcResult assigned =
        mvc.perform(
                post("/api/v1/tickets/" + ticketId + "/assignments")
                    .header("Authorization", bearer(supportToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"assigneeId\":\""
                            + supportId
                            + "\",\"expectedVersion\":"
                            + ticketVersion
                            + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(supportId))
            .andReturn();
    ticketVersion = body(assigned).get("version").asLong();

    mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/assignments")
                .header("Authorization", bearer(supportToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"assigneeId\":\""
                        + supportId
                        + "\",\"expectedVersion\":"
                        + staleVersion
                        + "}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TICKET_VERSION_CONFLICT"));
  }

  @Test
  @Order(5)
  void employeeAndSupportAppendAndReadComments() throws Exception {
    mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/comments")
                .header("Authorization", bearer(employeeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"content\":\"I can reproduce this every morning.\",\"expectedVersion\":"
                        + ticketVersion
                        + "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorId").value(employeeId));
    ticketVersion = currentVersion(employeeToken);

    mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/comments")
                .header("Authorization", bearer(supportToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"content\":\"We are investigating the login gateway.\",\"expectedVersion\":"
                        + ticketVersion
                        + "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorId").value(supportId));
    ticketVersion = currentVersion(supportToken);

    mvc.perform(
            get("/api/v1/tickets/" + ticketId + "/comments")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].content").value("I can reproduce this every morning."));
  }

  @Test
  @Order(6)
  void assignedSupportFollowsStatusMachineAndInvalidTransitionConflicts() throws Exception {
    MvcResult progress = transition("IN_PROGRESS", ticketVersion, 200);
    ticketVersion = body(progress).get("version").asLong();

    transition("CLOSED", ticketVersion, 409);

    MvcResult resolved = transition("RESOLVED", ticketVersion, 200);
    ticketVersion = body(resolved).get("version").asLong();
    MvcResult closed = transition("CLOSED", ticketVersion, 200);
    ticketVersion = body(closed).get("version").asLong();
    assertThat(body(closed).get("status").asText()).isEqualTo("CLOSED");
  }

  @Test
  @Order(7)
  void ticketAuditIsCompleteAndDoesNotContainBusinessContentOrSecrets() {
    List<AuditLog> ticketAudits =
        audits.findAll().stream().filter(x -> "TICKET".equals(x.getResourceType())).toList();
    assertThat(ticketAudits.stream().map(AuditLog::getAction))
        .contains(
            AuditAction.TICKET_CREATED,
            AuditAction.TICKET_ASSIGNED,
            AuditAction.TICKET_STATUS_CHANGED,
            AuditAction.TICKET_COMMENT_ADDED);
    assertThat(ticketAudits)
        .allSatisfy(
            event -> {
              assertThat(event.getActorId()).isNotNull();
              assertThat(event.getRequestId()).isNotBlank();
              String metadata = event.getMetadataJson().toString().toLowerCase(Locale.ROOT);
              assertThat(metadata)
                  .doesNotContain(
                      "description", "content", "password", "authorization", "jwt", "refresh");
            });
  }

  private String createUser(String email, String displayName, String password, String role)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/users")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\""
                            + email
                            + "\",\"displayName\":\""
                            + displayName
                            + "\",\"initialPassword\":\""
                            + password
                            + "\",\"roles\":[\""
                            + role
                            + "\"]}"))
            .andExpect(status().isCreated())
            .andReturn();
    return body(result).get("id").asText();
  }

  private String login(String email, String password) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("accessToken").asText();
  }

  private long currentVersion(String token) throws Exception {
    MvcResult result =
        mvc.perform(get("/api/v1/tickets/" + ticketId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("version").asLong();
  }

  private MvcResult transition(String status, long version, int expectedHttpStatus)
      throws Exception {
    return mvc.perform(
            post("/api/v1/tickets/" + ticketId + "/transitions")
                .header("Authorization", bearer(supportToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\",\"expectedVersion\":" + version + "}"))
        .andExpect(status().is(expectedHttpStatus))
        .andReturn();
  }

  private int tableCount(String table) {
    return jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_schema=database() and table_name=?",
        Integer.class,
        table);
  }

  private int indexCount(String table, String index) {
    return jdbc.queryForObject(
        "select count(*) from information_schema.statistics where table_schema=database() and table_name=? and index_name=?",
        Integer.class,
        table,
        index);
  }

  private int foreignKeyCount(String table, String key) {
    return jdbc.queryForObject(
        "select count(*) from information_schema.table_constraints where constraint_schema=database() and table_name=? and constraint_name=? and constraint_type='FOREIGN KEY'",
        Integer.class,
        table,
        key);
  }

  private int primaryKeyCount(String table) {
    return jdbc.queryForObject(
        "select count(*) from information_schema.table_constraints where constraint_schema=database() and table_name=? and constraint_type='PRIMARY KEY'",
        Integer.class,
        table);
  }

  private JsonNode body(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
