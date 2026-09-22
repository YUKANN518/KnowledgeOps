package com.knowledgeops.knowledge.application;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  StoredFile store(MultipartFile file);

  byte[] read(String storageKey);

  void delete(String storageKey);

  record StoredFile(
      String originalFilename,
      String storedFilename,
      String storageKey,
      String contentType,
      long fileSize) {}
}
