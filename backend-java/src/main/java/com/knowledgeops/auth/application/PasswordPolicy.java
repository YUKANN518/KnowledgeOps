package com.knowledgeops.auth.application;

import com.knowledgeops.shared.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
  public void validate(String password) {
    if (password == null || password.length() < 12 || password.length() > 72)
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Password must be between 12 and 72 characters");
  }
}
