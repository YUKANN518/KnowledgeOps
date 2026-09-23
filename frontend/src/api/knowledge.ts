import { api } from './client'
import type { ArticleStatus, DocumentStatus, KnowledgeArticle, KnowledgeCategory, KnowledgeDocument, PageResponse } from '../types'

export const knowledgeApi = {
  categories: () => api.get<KnowledgeCategory[]>('/knowledge/categories'),
  createCategory: (body: { name: string; description: string }) => api.post<KnowledgeCategory>('/knowledge/categories', body),
  articles: (params: { page: number; size: number; categoryId?: string; status?: ArticleStatus }) => api.get<PageResponse<KnowledgeArticle>>('/knowledge/articles', { params }),
  article: (id: string) => api.get<KnowledgeArticle>(`/knowledge/articles/${id}`),
  createArticle: (body: { title: string; content: string; categoryId: string }) => api.post<KnowledgeArticle>('/knowledge/articles', body),
  updateArticle: (id: string, body: { title: string; content: string; categoryId: string; expectedVersion: number }) => api.put<KnowledgeArticle>(`/knowledge/articles/${id}`, body),
  publishArticle: (id: string, expectedVersion: number) => api.post<KnowledgeArticle>(`/knowledge/articles/${id}/publish`, { expectedVersion }),
  archiveArticle: (id: string, expectedVersion: number) => api.post<KnowledgeArticle>(`/knowledge/articles/${id}/archive`, { expectedVersion }),
  documents: (params: { page: number; size: number; categoryId?: string; status?: DocumentStatus }) => api.get<PageResponse<KnowledgeDocument>>('/documents', { params }),
  upload: (categoryId: string, file: File) => {
    const data = new FormData(); data.append('categoryId', categoryId); data.append('file', file)
    return api.post<KnowledgeDocument>('/documents', data)
  },
  archiveDocument: (id: string) => api.post<KnowledgeDocument>(`/documents/${id}/archive`),
  download: (id: string) => api.get<Blob>(`/documents/${id}/content`, { responseType: 'blob' }),
}
