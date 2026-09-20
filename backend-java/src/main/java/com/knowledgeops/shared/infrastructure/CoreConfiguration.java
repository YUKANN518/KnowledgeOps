package com.knowledgeops.shared.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class CoreConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
