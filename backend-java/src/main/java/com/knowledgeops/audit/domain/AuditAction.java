package com.knowledgeops.audit.domain;

public enum AuditAction {
  BOOTSTRAP_ADMIN,
  LOGIN_SUCCESS,
  LOGIN_FAILURE,
  REFRESH,
  REFRESH_REPLAY,
  LOGOUT,
  USER_CREATE,
  USER_DISABLE,
  ROLE_ASSIGN,
  PERMISSION_CHANGE
}
