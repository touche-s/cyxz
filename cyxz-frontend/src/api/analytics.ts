import request from '@/utils/request'

export interface DashboardVO {
  newUsers: number
  newPosts: number
  approvedPosts: number
  rejectedPosts: number
  newCircles: number
  newJoins: number
  reportHandled: number
}

export interface TrendVO {
  date: string
  value: number
}

export const getDashboard = (): Promise<DashboardVO> => {
  return request.get('/admin/analytics/dashboard')
}

export const getTrend = (params: { metric: string; days?: number }): Promise<TrendVO[]> => {
  return request.get('/admin/analytics/trend', { params })
}
