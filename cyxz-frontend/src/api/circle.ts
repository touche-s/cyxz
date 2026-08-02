import request from '@/utils/request'

export interface CircleVO {
  id: number
  name: string
  slug: string
  intro: string
  avatar: string
  cover: string
  postCount: number
  memberCount: number
  joined: boolean
}

export const getCircleList = (): Promise<CircleVO[]> => {
  return request.get('/circle/list')
}

export const getCircleDetail = (circleId: number): Promise<CircleVO> => {
  return request.get(`/circle/${circleId}`)
}

export const getJoinedCircles = (): Promise<CircleVO[]> => {
  return request.get('/circle/joined')
}

export const getHotCircles = (): Promise<CircleVO[]> => {
  return request.get('/circle/hot', { params: { page: 1, size: 20 } })
}

export const joinCircle = (circleId: number) => {
  return request.post(`/circle/${circleId}/join`)
}

export const leaveCircle = (circleId: number) => {
  return request.delete(`/circle/${circleId}/join`)
}

export const updateCircle = (circleId: number, data: { name?: string; intro?: string; avatar?: string; cover?: string }) => {
  return request.put(`/circle/${circleId}`, null, { params: data })
}

export interface CircleSectionVO {
  /** circle_section 主键 */
  id: number
  /** 所属圈子 ID */
  circleId: number
  /** 关联的模板 ID */
  templateId: number
  /** 板块名称，来自 section_template 表 */
  name: string
  /** 适用类型：ALL/NORMAL/ARTICLE */
  applicableType: string
  /** 是否默认板块：1=是 */
  isDefault: number
  /** 排序值 */
  sortOrder: number
  /** 状态：1=启用 */
  status: number
}

/** 获取圈子已启用的板块列表，发帖选择板块时调用 */
export const getCircleSections = (circleId: number): Promise<CircleSectionVO[]> => {
  return request.get(`/circle/${circleId}/sections`)
}
