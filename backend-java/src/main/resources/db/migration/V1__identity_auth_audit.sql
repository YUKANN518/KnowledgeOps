CREATE TABLE departments (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE users (
  id CHAR(36) PRIMARY KEY,
  department_id CHAR(36) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  status VARCHAR(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','DISABLED','LOCKED')),
  INDEX ix_users_department_status (department_id, status, id)
) ENGINE=InnoDB;

CREATE TABLE roles (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE permissions (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE user_roles (
  user_id CHAR(36) NOT NULL,
  role_id CHAR(36) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
  role_id CHAR(36) NOT NULL,
  permission_id CHAR(36) NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  family_id CHAR(36) NOT NULL,
  token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6),
  revoked_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_refresh_family (family_id, revoked_at),
  INDEX ix_refresh_user_active (user_id, revoked_at, expires_at),
  INDEX ix_refresh_expiry (expires_at)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id CHAR(36) PRIMARY KEY,
  actor_type VARCHAR(16) NOT NULL,
  actor_id CHAR(36),
  action VARCHAR(80) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64),
  metadata_json JSON NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  CONSTRAINT ck_audit_actor_type CHECK (actor_type IN ('USER','SYSTEM','ANONYMOUS')),
  INDEX ix_audit_actor (actor_id, created_at, id),
  INDEX ix_audit_resource (resource_type, resource_id, created_at, id),
  INDEX ix_audit_request (request_id, created_at),
  INDEX ix_audit_time (created_at, id)
) ENGINE=InnoDB;

INSERT INTO departments (id, code, name, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'GENERAL', 'General', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO permissions (id, code, description) VALUES
('10000000-0000-0000-0000-000000000001', 'user.read', 'Read user profiles in scope'),
('10000000-0000-0000-0000-000000000002', 'user.manage', 'Create, disable, and assign roles'),
('10000000-0000-0000-0000-000000000003', 'audit.read', 'Read redacted audit records'),
('10000000-0000-0000-0000-000000000004', 'ticket.create', 'Future ticket creation permission'),
('10000000-0000-0000-0000-000000000005', 'ticket.read.own', 'Future own-ticket read permission'),
('10000000-0000-0000-0000-000000000006', 'knowledge.read', 'Future knowledge read permission'),
('10000000-0000-0000-0000-000000000007', 'ai.use', 'Future AI usage permission');

INSERT INTO roles (id, code, name) VALUES
('20000000-0000-0000-0000-000000000001', 'EMPLOYEE', 'Employee'),
('20000000-0000-0000-0000-000000000002', 'SUPPORT_AGENT', 'Support Agent'),
('20000000-0000-0000-0000-000000000003', 'KNOWLEDGE_MANAGER', 'Knowledge Manager'),
('20000000-0000-0000-0000-000000000004', 'ADMINISTRATOR', 'Administrator');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('EMPLOYEE','SUPPORT_AGENT','KNOWLEDGE_MANAGER','ADMINISTRATOR')
  AND p.code IN ('ticket.create','ticket.read.own','knowledge.read','ai.use');
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATOR' AND p.code IN ('user.read','user.manage','audit.read');
