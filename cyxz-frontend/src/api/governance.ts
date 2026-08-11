import request from '@/utils/request'
import type { PageResult } from './types/common'

export interface ReportVO {
  id: number
  reporterId: number
  targetType: string
  targetId: number
  reason: string
  status: string
  handlerId: number | null
  handlerNote: string | null
  handledAt: string | null
  createTime: string
}

export const getReportList = (params: {
  status?: string
  targetType?: string
  page?: number
  size?: number
}): Promise<PageResult<ReportVO>> => {
  return request.get('/admin/report/list', { params })
}

export const getReportDetail = (id: number): Promise<ReportVO> => {
  return request.get(`/admin/report/${id}`)
}

export const approveReport = (id: number, note: string) => {
  return request.put(`/admin/report/${id}/approve`, { note })
}

export const rejectReport = (id: number, note: string) => {
  return request.put(`/admin/report/${id}/reject`, { note })
}
