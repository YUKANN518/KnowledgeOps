INSERT INTO permissions (id, code, description) VALUES
('10000000-0000-0000-0000-000000000015', 'knowledge.write', 'Create and manage knowledge content'),
('10000000-0000-0000-0000-000000000016', 'knowledge.admin', 'Administer all knowledge content');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'KNOWLEDGE_MANAGER' AND p.code = 'knowledge.write';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATOR' AND p.code IN ('knowledge.write', 'knowledge.admin');

CREATE TABLE knowledge_categories (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(1000),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_knowledge_categories_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE knowledge_articles (
  id CHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  category_id CHAR(36) NOT NULL,
  status VARCHAR(16) NOT NULL,
  author_id CHAR(36) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_knowledge_articles_category FOREIGN KEY (category_id) REFERENCES knowledge_categories(id),
  CONSTRAINT fk_knowledge_articles_author FOREIGN KEY (author_id) REFERENCES users(id),
  CONSTRAINT ck_knowledge_articles_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
  INDEX ix_knowledge_articles_status (status, created_at, id),
  INDEX ix_knowledge_articles_category (category_id, status, created_at, id),
  INDEX ix_knowledge_articles_author (author_id, created_at, id)
) ENGINE=InnoDB;

CREATE TABLE knowledge_documents (
  id CHAR(36) PRIMARY KEY,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(80) NOT NULL,
  storage_key VARCHAR(80) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  uploader_id CHAR(36) NOT NULL,
  category_id CHAR(36) NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_knowledge_documents_stored_filename UNIQUE (stored_filename),
  CONSTRAINT uk_knowledge_documents_storage_key UNIQUE (storage_key),
  CONSTRAINT fk_knowledge_documents_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
  CONSTRAINT fk_knowledge_documents_category FOREIGN KEY (category_id) REFERENCES knowledge_categories(id),
  CONSTRAINT ck_knowledge_documents_status CHECK (status IN ('ACTIVE','ARCHIVED')),
  CONSTRAINT ck_knowledge_documents_file_size CHECK (file_size > 0 AND file_size <= 20971520),
  INDEX ix_knowledge_documents_category (category_id, status, created_at, id),
  INDEX ix_knowledge_documents_created (created_at, id),
  INDEX ix_knowledge_documents_uploader (uploader_id, created_at, id)
) ENGINE=InnoDB;
