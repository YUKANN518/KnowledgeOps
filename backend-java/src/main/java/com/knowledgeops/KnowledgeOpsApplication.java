package com.knowledgeops;

import com.knowledgeops.auth.infrastructure.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
public class KnowledgeOpsApplication {
  public static void main(String[] args) {
    SpringApplication.run(KnowledgeOpsApplication.class, args);
  }
}
