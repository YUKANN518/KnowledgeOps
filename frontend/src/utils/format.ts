const displayLabels: Record<string, string> = {
  OPEN: '待处理',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
  ACTIVE: '有效',
  URGENT: '紧急',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  EMPLOYEE: '普通员工',
  SUPPORT_AGENT: '支持人员',
  KNOWLEDGE_MANAGER: '知识管理员',
  ADMINISTRATOR: '系统管理员',
  'user.read': '查看用户',
  'user.manage': '管理用户',
  'audit.read': '查看审计日志',
  'ticket.create': '创建工单',
  'ticket.read.own': '查看本人工单',
  'ticket.read.department': '查看部门工单',
  'ticket.read.all': '查看全部工单',
  'ticket.assign.department': '分配部门工单',
  'ticket.assign.all': '分配全部工单',
  'ticket.transition.assigned': '流转已分配工单',
  'ticket.transition.all': '流转全部工单',
  'ticket.comment.public': '添加工单评论',
  'knowledge.read': '查看知识内容',
  'knowledge.write': '编辑知识内容',
  'knowledge.admin': '管理知识内容',
  'ai.use': 'AI 权限（预留）',
}

export const displayLabel = (value: string) => displayLabels[value] ?? value.replaceAll('_', ' ')
export const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
export const shortId = (value?: string | null) => value ? `${value.slice(0, 8)}…` : '未分配'
export const formatBytes = (value: number) => value < 1024 ? `${value} B` : value < 1048576 ? `${(value / 1024).toFixed(1)} KB` : `${(value / 1048576).toFixed(1)} MB`
