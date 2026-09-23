export type Role = 'EMPLOYEE' | 'SUPPORT_AGENT' | 'KNOWLEDGE_MANAGER' | 'ADMINISTRATOR'
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type DocumentStatus = 'ACTIVE' | 'ARCHIVED'

export interface User {
  id: string; email: string; displayName: string; status: string; roles: Role[];
  permissions: string[]; version: number; createdAt: string; updatedAt: string
}
export interface AuthTokens { accessToken: string; expiresIn: number; csrfToken: string; user: User }
export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface Ticket {
  id: string; title: string; description?: string; priority: TicketPriority; status: TicketStatus;
  creatorId: string; assigneeId: string | null; version: number; createdAt: string; updatedAt: string
}
export interface TicketComment { id: string; ticketId: string; authorId: string; content: string; createdAt: string }
export interface KnowledgeCategory { id: string; name: string; description: string | null; createdAt: string; updatedAt: string }
export interface KnowledgeArticle {
  id: string; title: string; content?: string; categoryId: string; status: ArticleStatus;
  authorId: string; version: number; createdAt: string; updatedAt: string
}
export interface KnowledgeDocument {
  id: string; originalFilename: string; contentType: string; fileSize: number; uploaderId: string;
  categoryId: string; status: DocumentStatus; createdAt: string; updatedAt: string
}
export interface ApiErrorBody { code: string; message: string; details: Array<{field: string; message: string}>; requestId: string }
