package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.application.FileStorageService;
import com.knowledgeops.shared.domain.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {
  private static final long MAX_BYTES = 20L * 1024 * 1024;
  private final Path root;

  public LocalFileStorageService(
      @Value("${knowledgeops.knowledge.storage-path:./data/uploads}") String path) {
    try {
      root = Paths.get(path).toAbsolutePath().normalize();
      Files.createDirectories(root);
      if (Files.isSymbolicLink(root)) throw storageError("Storage root cannot be a symbolic link");
    } catch (IOException | InvalidPathException ex) {
      throw storageError("Cannot initialize document storage", ex);
    }
  }

  @Override
  public StoredFile store(MultipartFile file) {
    String original = Objects.requireNonNullElse(file.getOriginalFilename(), "").trim();
    validateFilename(original);
    if (file.isEmpty()) throw unsupported("File must not be empty");
    if (file.getSize() > MAX_BYTES) {
      throw new BusinessException(
          ErrorCode.FILE_TOO_LARGE, HttpStatus.UNPROCESSABLE_ENTITY, "File exceeds 20 MiB");
    }
    String extension = extension(original);
    validateType(file, extension);
    String stored = UUID.randomUUID() + extension;
    Path target = safePath(stored);
    try {
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS))
        throw storageError("Storage key collision");
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, target);
      }
      if (Files.isSymbolicLink(target)) {
        Files.deleteIfExists(target);
        throw storageError("Stored file cannot be a symbolic link");
      }
      return new StoredFile(
          original, stored, stored, normalizedContentType(extension), file.getSize());
    } catch (IOException ex) {
      try {
        Files.deleteIfExists(target);
      } catch (IOException ignored) {
      }
      throw storageError("Cannot store document", ex);
    }
  }

  @Override
  public byte[] read(String storageKey) {
    Path target = safePath(storageKey);
    try {
      if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(target))
        throw storageError("Stored document is unavailable");
      return Files.readAllBytes(target);
    } catch (IOException ex) {
      throw storageError("Cannot read document", ex);
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(safePath(storageKey));
    } catch (IOException ex) {
      throw storageError("Cannot clean up document", ex);
    }
  }

  private void validateFilename(String name) {
    if (name.isBlank()
        || name.length() > 255
        || name.equals(".")
        || name.equals("..")
        || name.contains("/")
        || name.contains("\\")
        || Paths.get(name).isAbsolute()) throw unsupported("Unsafe file name");
  }

  private String extension(String name) {
    int dot = name.lastIndexOf('.');
    if (dot < 1) throw unsupported("File extension is required");
    String value = name.substring(dot).toLowerCase(Locale.ROOT);
    if (!Set.of(".pdf", ".docx", ".txt", ".md", ".markdown").contains(value))
      throw unsupported("Supported file types are PDF, DOCX, TXT and Markdown");
    return value;
  }

  private void validateType(MultipartFile file, String extension) {
    String mime = Objects.requireNonNullElse(file.getContentType(), "").toLowerCase(Locale.ROOT);
    try (InputStream in = file.getInputStream()) {
      byte[] prefix = in.readNBytes(8);
      boolean valid =
          switch (extension) {
            case ".pdf" ->
                mime.equals("application/pdf")
                    && prefix.length >= 5
                    && new String(prefix, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
            case ".docx" ->
                (mime.equals(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        || mime.equals("application/zip"))
                    && prefix.length >= 4
                    && prefix[0] == 'P'
                    && prefix[1] == 'K'
                    && prefix[2] == 3
                    && prefix[3] == 4;
            default ->
                (mime.equals("text/plain")
                        || mime.equals("text/markdown")
                        || mime.equals("application/octet-stream"))
                    && isText(file);
          };
      if (!valid) throw unsupported("File content does not match its extension and media type");
    } catch (IOException ex) {
      throw storageError("Cannot inspect document", ex);
    }
  }

  private boolean isText(MultipartFile file) throws IOException {
    byte[] bytes = file.getBytes();
    for (byte value : bytes) if (value == 0) return false;
    try {
      StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes));
      return true;
    } catch (CharacterCodingException ex) {
      return false;
    }
  }

  private String normalizedContentType(String extension) {
    return switch (extension) {
      case ".pdf" -> "application/pdf";
      case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case ".md", ".markdown" -> "text/markdown";
      default -> "text/plain";
    };
  }

  private Path safePath(String storageKey) {
    if (storageKey == null
        || storageKey.isBlank()
        || storageKey.contains("/")
        || storageKey.contains("\\")) throw storageError("Invalid storage key");
    Path resolved = root.resolve(storageKey).normalize();
    if (!resolved.startsWith(root) || !Objects.equals(resolved.getParent(), root))
      throw storageError("Invalid storage key");
    return resolved;
  }

  private BusinessException unsupported(String message) {
    return new BusinessException(
        ErrorCode.UNSUPPORTED_FILE_TYPE, HttpStatus.UNPROCESSABLE_ENTITY, message);
  }

  private BusinessException storageError(String message) {
    return storageError(message, null);
  }

  private BusinessException storageError(String message, Exception cause) {
    return new BusinessException(
        ErrorCode.FILE_STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }
}
