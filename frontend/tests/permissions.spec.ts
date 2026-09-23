import { describe, expect, it } from 'vitest'
import { canAccessRoute, canManageKnowledge, hasAnyPermission, hasPermission } from '../src/utils/permissions'
import type { User } from '../src/types'

const user = (permissions: string[]): User => ({
  id: 'user-id',
  email: 'user@example.com',
  displayName: 'Test User',
  status: 'ACTIVE',
  roles: ['EMPLOYEE'],
  permissions,
  version: 0,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
})

describe('permission helpers', () => {
  it('checks individual and alternative permissions', () => {
    const current = user(['knowledge.read', 'ticket.create'])
    expect(hasPermission(current, 'knowledge.read')).toBe(true)
    expect(hasPermission(current, 'knowledge.write')).toBe(false)
    expect(hasAnyPermission(current, ['knowledge.write', 'ticket.create'])).toBe(true)
  })

  it('limits knowledge management controls to writers and admins', () => {
    expect(canManageKnowledge(user(['knowledge.read']))).toBe(false)
    expect(canManageKnowledge(user(['knowledge.write']))).toBe(true)
    expect(canManageKnowledge(user(['knowledge.admin']))).toBe(true)
  })

  it('enforces route requirements with all or any permission semantics', () => {
    const current = user(['knowledge.read', 'ticket.create'])
    expect(canAccessRoute(current, [])).toBe(true)
    expect(canAccessRoute(current, ['knowledge.read', 'ticket.create'])).toBe(true)
    expect(canAccessRoute(current, ['knowledge.read', 'knowledge.write'])).toBe(false)
    expect(canAccessRoute(current, ['knowledge.write', 'ticket.create'], true)).toBe(true)
    expect(canAccessRoute(null, ['knowledge.read'])).toBe(false)
  })
})
