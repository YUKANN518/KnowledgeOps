INSERT INTO permissions (id, code, description) VALUES
('10000000-0000-0000-0000-000000000008', 'ticket.read.department', 'Read tickets in the actor department'),
('10000000-0000-0000-0000-000000000009', 'ticket.read.all', 'Read all tickets'),
('10000000-0000-0000-0000-000000000010', 'ticket.assign.department', 'Assign tickets in the actor department'),
('10000000-0000-0000-0000-000000000011', 'ticket.assign.all', 'Assign all tickets'),
('10000000-0000-0000-0000-000000000012', 'ticket.transition.assigned', 'Transition assigned tickets'),
('10000000-0000-0000-0000-000000000013', 'ticket.transition.all', 'Transition all tickets'),
('10000000-0000-0000-0000-000000000014', 'ticket.comment.public', 'Add public ticket comments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('EMPLOYEE', 'SUPPORT_AGENT', 'KNOWLEDGE_MANAGER', 'ADMINISTRATOR')
  AND p.code = 'ticket.comment.public';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPPORT_AGENT'
  AND p.code IN ('ticket.read.department', 'ticket.assign.department', 'ticket.transition.assigned');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATOR'
  AND p.code IN ('ticket.read.all', 'ticket.assign.all', 'ticket.transition.all');

CREATE TABLE tickets (
  id CHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  priority VARCHAR(16) NOT NULL,
  status VARCHAR(24) NOT NULL,
  creator_id CHAR(36) NOT NULL,
  department_id CHAR(36) NOT NULL,
  assignee_id CHAR(36),
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_tickets_creator FOREIGN KEY (creator_id) REFERENCES users(id),
  CONSTRAINT fk_tickets_department FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT fk_tickets_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
  CONSTRAINT ck_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
  CONSTRAINT ck_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
  INDEX ix_tickets_creator (creator_id, status, created_at, id),
  INDEX ix_tickets_department (department_id, status, created_at, id),
  INDEX ix_tickets_assignee (assignee_id, status, updated_at, id),
  INDEX ix_tickets_status_created (status, created_at, id)
) ENGINE=InnoDB;

CREATE TABLE ticket_comments (
  id CHAR(36) PRIMARY KEY,
  ticket_id CHAR(36) NOT NULL,
  author_id CHAR(36) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
  CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
  INDEX ix_ticket_comments_ticket (ticket_id, created_at, id)
) ENGINE=InnoDB;
