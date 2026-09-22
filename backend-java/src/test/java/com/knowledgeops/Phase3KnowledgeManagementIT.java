package com.knowledgeops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.knowledgeops.audit.domain.*;
import com.knowledgeops.audit.infrastructure.AuditLogRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
class Phase3KnowledgeManagementIT {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", "test-redis-password");

  @TempDir static Path storageDirectory;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
    r.add("spring.datasource.username", MYSQL::getUsername);
    r.add("spring.datasource.password", MYSQL::getPassword);
    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    r.add("spring.data.redis.password", () -> "test-redis-password");
    r.add("knowledgeops.knowledge.storage-path", () -> storageDirectory.toString());
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
  static String managerToken;
  static String employeeToken;
  static String managerId;
  static String employeeId;
  static String categoryId;
  static String articleId;
  static long articleVersion;
  static String pdfDocumentId;
  static String textDocumentId;

  @Test
  @Order(1)
  void migrationUsersPermissionsAndCategoryAreReady() throws Exception {
    adminToken = login("admin@example.local", "AdminPassword!123");
    managerId =
        createUser(
            "phase3.manager@example.local",
            "Knowledge Manager",
            "ManagerPassword!123",
            "KNOWLEDGE_MANAGER");
    employeeId =
        createUser(
            "phase3.employee@example.local",
            "Knowledge Reader",
            "EmployeePassword!123",
            "EMPLOYEE");
    managerToken = login("phase3.manager@example.local", "ManagerPassword!123");
    employeeToken = login("phase3.employee@example.local", "EmployeePassword!123");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version='3' and success=1",
                Integer.class))
        .isEqualTo(1);
    assertThat(tableCount("knowledge_categories")).isEqualTo(1);
    assertThat(tableCount("knowledge_articles")).isEqualTo(1);
    assertThat(tableCount("knowledge_documents")).isEqualTo(1);
    assertThat(indexCount("knowledge_articles", "ix_knowledge_articles_status")).isGreaterThan(0);
    assertThat(indexCount("knowledge_documents", "ix_knowledge_documents_category"))
        .isGreaterThan(0);
    assertThat(foreignKeyCount("knowledge_articles", "fk_knowledge_articles_category"))
        .isEqualTo(1);
    assertThat(foreignKeyCount("knowledge_documents", "fk_knowledge_documents_uploader"))
        .isEqualTo(1);

    MvcResult created =
        mvc.perform(
                post("/api/v1/knowledge/categories")
                    .header("Authorization", bearer(managerToken))
                    .header("X-Request-ID", "phase3-category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Operations\",\"description\":\"Runbooks and policies\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Operations"))
            .andReturn();
    categoryId = body(created).get("id").asText();

    mvc.perform(
            post("/api/v1/knowledge/categories")
                .header("Authorization", bearer(employeeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forbidden\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(2)
  void managerCreatesUpdatesPublishesAndEmployeeReadsOnlyPublishedArticle() throws Exception {
    mvc.perform(get("/api/v1/knowledge/articles")).andExpect(status().isUnauthorized());

    MvcResult created =
        mvc.perform(
                post("/api/v1/knowledge/articles")
                    .header("Authorization", bearer(managerToken))
                    .header("X-Request-ID", "phase3-article-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"VPN setup\",\"content\":\"Draft instructions\",\"categoryId\":\""
                            + categoryId
                            + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.authorId").value(managerId))
            .andReturn();
    JsonNode article = body(created);
    articleId = article.get("id").asText();
    articleVersion = article.get("version").asLong();

    mvc.perform(
            get("/api/v1/knowledge/articles/" + articleId)
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("KNOWLEDGE_ACCESS_DENIED"));
    mvc.perform(
            get("/api/v1/knowledge/articles")
                .queryParam("status", "DRAFT")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(
            put("/api/v1/knowledge/articles/" + articleId)
                .header("Authorization", bearer(employeeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateArticleJson("Employee edit", "Forbidden", articleVersion)))
        .andExpect(status().isForbidden());

    MvcResult updated =
        mvc.perform(
                put("/api/v1/knowledge/articles/" + articleId)
                    .header("Authorization", bearer(managerToken))
                    .header("X-Request-ID", "phase3-article-update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        updateArticleJson(
                            "VPN setup guide", "Approved instructions", articleVersion)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("VPN setup guide"))
            .andReturn();
    long staleVersion = articleVersion;
    articleVersion = body(updated).get("version").asLong();

    mvc.perform(
            put("/api/v1/knowledge/articles/" + articleId)
                .header("Authorization", bearer(managerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateArticleJson("Stale", "Stale", staleVersion)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ARTICLE_VERSION_CONFLICT"));

    MvcResult published =
        mvc.perform(
                post("/api/v1/knowledge/articles/" + articleId + "/publish")
                    .header("Authorization", bearer(managerToken))
                    .header("X-Request-ID", "phase3-article-publish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"expectedVersion\":" + articleVersion + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andReturn();
    articleVersion = body(published).get("version").asLong();

    mvc.perform(
            get("/api/v1/knowledge/articles/" + articleId)
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Approved instructions"));
    mvc.perform(
            get("/api/v1/knowledge/articles")
                .queryParam("categoryId", categoryId)
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
  }

  @Test
  @Order(3)
  void uploadsDownloadsValidatesAndArchivesDocuments() throws Exception {
    MockMultipartFile pdf =
        new MockMultipartFile(
            "file",
            "vpn-policy.pdf",
            "application/pdf",
            "%PDF-1.4\nsynthetic".getBytes(StandardCharsets.US_ASCII));
    MvcResult uploadedPdf =
        mvc.perform(
                multipart("/api/v1/documents")
                    .file(pdf)
                    .param("categoryId", categoryId)
                    .header("Authorization", bearer(managerToken))
                    .header("X-Request-ID", "phase3-pdf-upload"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalFilename").value("vpn-policy.pdf"))
            .andExpect(jsonPath("$.storageKey").doesNotExist())
            .andReturn();
    pdfDocumentId = body(uploadedPdf).get("id").asText();

    MockMultipartFile text =
        new MockMultipartFile(
            "file",
            "readme.txt",
            "text/plain",
            "synthetic knowledge".getBytes(StandardCharsets.UTF_8));
    MvcResult uploadedText =
        mvc.perform(
                multipart("/api/v1/documents")
                    .file(text)
                    .param("categoryId", categoryId)
                    .header("Authorization", bearer(managerToken)))
            .andExpect(status().isCreated())
            .andReturn();
    textDocumentId = body(uploadedText).get("id").asText();

    mvc.perform(
            get("/api/v1/documents/" + textDocumentId + "/content")
                .header("Authorization", bearer(employeeToken))
                .header("X-Request-ID", "phase3-document-download"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(
            header()
                .string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
        .andExpect(content().bytes("synthetic knowledge".getBytes(StandardCharsets.UTF_8)));

    mvc.perform(
            multipart("/api/v1/documents")
                .file(text)
                .param("categoryId", categoryId)
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isForbidden());
    mvc.perform(multipart("/api/v1/documents").file(text).param("categoryId", categoryId))
        .andExpect(status().isUnauthorized());

    MockMultipartFile executable =
        new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", new byte[] {1, 2, 3});
    rejectedUpload(executable, "UNSUPPORTED_FILE_TYPE");
    MockMultipartFile traversal =
        new MockMultipartFile(
            "file", "../escape.txt", "text/plain", "escape".getBytes(StandardCharsets.UTF_8));
    rejectedUpload(traversal, "UNSUPPORTED_FILE_TYPE");
    assertThat(Files.exists(storageDirectory.getParent().resolve("escape.txt"))).isFalse();
    MockMultipartFile fakePdf =
        new MockMultipartFile(
            "file", "fake.pdf", "application/pdf", "not pdf".getBytes(StandardCharsets.UTF_8));
    rejectedUpload(fakePdf, "UNSUPPORTED_FILE_TYPE");

    byte[] tooLarge = new byte[20 * 1024 * 1024 + 1];
    Arrays.fill(tooLarge, (byte) 'a');
    MockMultipartFile oversized =
        new MockMultipartFile("file", "large.txt", "text/plain", tooLarge);
    rejectedUpload(oversized, "FILE_TOO_LARGE");

    mvc.perform(
            get("/api/v1/documents/" + UUID.randomUUID() + "/content")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("KNOWLEDGE_DOCUMENT_NOT_FOUND"));

    mvc.perform(
            post("/api/v1/documents/" + pdfDocumentId + "/archive")
                .header("Authorization", bearer(managerToken))
                .header("X-Request-ID", "phase3-document-archive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
    mvc.perform(
            get("/api/v1/documents/" + pdfDocumentId + "/content")
                .header("Authorization", bearer(employeeToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("KNOWLEDGE_ACCESS_DENIED"));
    mvc.perform(
            get("/api/v1/documents/" + pdfDocumentId + "/content")
                .header("Authorization", bearer(managerToken)))
        .andExpect(status().isOk())
        .andExpect(content().bytes("%PDF-1.4\nsynthetic".getBytes(StandardCharsets.US_ASCII)));

    try (var files = Files.list(storageDirectory)) {
      assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(2);
    }
  }

  @Test
  @Order(4)
  void articleArchiveAndAuditEventsAreCompleteAndSafe() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge/articles/" + articleId + "/archive")
                .header("Authorization", bearer(managerToken))
                .header("X-Request-ID", "phase3-article-archive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":" + articleVersion + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));

    List<AuditLog> events =
        audits.findAll().stream()
            .filter(event -> event.getResourceType().startsWith("KNOWLEDGE_"))
            .toList();
    assertThat(events.stream().map(AuditLog::getAction))
        .contains(
            AuditAction.KNOWLEDGE_ARTICLE_CREATED,
            AuditAction.KNOWLEDGE_ARTICLE_UPDATED,
            AuditAction.KNOWLEDGE_ARTICLE_PUBLISHED,
            AuditAction.KNOWLEDGE_ARTICLE_ARCHIVED,
            AuditAction.KNOWLEDGE_DOCUMENT_UPLOADED,
            AuditAction.KNOWLEDGE_DOCUMENT_DOWNLOADED,
            AuditAction.KNOWLEDGE_DOCUMENT_ARCHIVED);
    assertThat(events)
        .allSatisfy(
            event -> {
              assertThat(event.getActorId()).isNotNull();
              assertThat(event.getRequestId()).isNotBlank();
              String metadata = event.getMetadataJson().toString().toLowerCase(Locale.ROOT);
              assertThat(event.getMetadataJson().keySet())
                  .doesNotContain(
                      "content",
                      "title",
                      "originalFilename",
                      "storedFilename",
                      "storageKey",
                      "password",
                      "authorization",
                      "jwt",
                      "refreshToken");
              assertThat(metadata)
                  .doesNotContain("approved instructions", "synthetic knowledge", "bearer ");
            });
  }

  private void rejectedUpload(MockMultipartFile file, String errorCode) throws Exception {
    mvc.perform(
            multipart("/api/v1/documents")
                .file(file)
                .param("categoryId", categoryId)
                .header("Authorization", bearer(managerToken)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(errorCode));
  }

  private String updateArticleJson(String title, String content, long version) {
    return "{\"title\":\""
        + title
        + "\",\"content\":\""
        + content
        + "\",\"categoryId\":\""
        + categoryId
        + "\",\"expectedVersion\":"
        + version
        + "}";
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

  private JsonNode body(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
