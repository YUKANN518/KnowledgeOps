import { api } from './client'
import type { PageResponse, Ticket, TicketComment, TicketPriority, TicketStatus } from '../types'

export const ticketApi = {
  list: (params: { page: number; size: number; status?: TicketStatus; priority?: TicketPriority }) => api.get<PageResponse<Ticket>>('/tickets', { params }),
  get: (id: string) => api.get<Ticket>(`/tickets/${id}`),
  create: (body: { title: string; description: string; priority: TicketPriority }) => api.post<Ticket>('/tickets', body),
  comments: (id: string) => api.get<TicketComment[]>(`/tickets/${id}/comments`),
  comment: (id: string, content: string, expectedVersion: number) => api.post<TicketComment>(`/tickets/${id}/comments`, { content, expectedVersion }),
  assign: (id: string, assigneeId: string, expectedVersion: number) => api.post<Ticket>(`/tickets/${id}/assignments`, { assigneeId, expectedVersion }),
  transition: (id: string, status: TicketStatus, expectedVersion: number) => api.post<Ticket>(`/tickets/${id}/transitions`, { status, expectedVersion }),
}
