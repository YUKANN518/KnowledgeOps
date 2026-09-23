import type { User } from '../types'

export const hasPermission = (user: User | null, permission: string) => Boolean(user?.permissions.includes(permission))
export const hasAnyPermission = (user: User | null, permissions: string[]) => permissions.some((item) => hasPermission(user, item))
export const canManageKnowledge = (user: User | null) => hasAnyPermission(user, ['knowledge.write', 'knowledge.admin'])
export const canAccessRoute = (user: User | null, permissions: string[], anyPermission = false) =>
  permissions.length === 0 || (anyPermission ? hasAnyPermission(user, permissions) : permissions.every((item) => hasPermission(user, item)))
