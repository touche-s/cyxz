import request from '@/utils/request'
import type { PageResult } from './types/common'

export interface AuditLogVO {
  id: number
  operatorId: number
  operatorName: string
  action: string
  targetType: string
  targetId: number
  detail: string
  ip: string
  createTime: string
}

export const getAuditLogList = (params: {
  action?: string
  targetType?: string
  operatorId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}): Promise<PageResult<AuditLogVO>> => {
  return request.get('/admin/audit/list', { params })
}
