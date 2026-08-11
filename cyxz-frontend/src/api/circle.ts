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
  status: number
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

/** 查询当前用户管理的圈子（圈主或圈子管理员），用于圈子管理后台左侧圈子选择器 */
export const getManagedCircles = (): Promise<CircleVO[]> => {
  return request.get('/circle/managed')
}

/** 管理员全量圈子列表（含禁用状态），用于平台管理后台 */
export const getAdminCircleList = (): Promise<CircleVO[]> => {
  return request.get('/circle/admin/list')
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

// ===== 圈子成员管理 =====

export interface MemberVO {
  userId: number
  username: string
  nickname: string
  avatar: string
  roleCode: string  // CIRCLE_OWNER / CIRCLE_ADMIN / CIRCLE_MEMBER
  roleLabel: string // 圈主 / 圈子管理员 / 圈子成员
  joinTime: string
}

/** 查询圈子成员列表 */
export const getCircleMembers = (circleId: number): Promise<MemberVO[]> => {
  return request.get(`/circle/${circleId}/members`)
}

/** 任命圈子管理员 */
export const appointAdmin = (circleId: number, userId: number) => {
  return request.put(`/circle/${circleId}/members/${userId}/promote`)
}

/** 撤销圈子管理员 */
export const removeAdmin = (circleId: number, userId: number) => {
  return request.put(`/circle/${circleId}/members/${userId}/demote`)
}

/** 移除圈子成员（踢出） */
export const kickMember = (circleId: number, userId: number) => {
  return request.delete(`/circle/${circleId}/members/${userId}`)
}

// ===== 建圈申请审核（管理员） =====

export interface CircleApplicationVO {
  id: number
  applicantId: number
  name: string
  intro: string
  avatar: string
  cover: string
  status: string
  reviewerId: number | null
  reviewNote: string | null
  reviewedAt: string | null
  createTime: string
}

export const getCircleApplications = (params: {
  status?: string
  page?: number
  size?: number
}) => {
  return request.get('/admin/circle-application/list', { params })
}

export const getCircleApplicationDetail = (id: number): Promise<CircleApplicationVO> => {
  return request.get(`/admin/circle-application/${id}`)
}

export const approveCircleApplication = (id: number, note: string) => {
  return request.put(`/admin/circle-application/${id}/approve`, { note })
}

export const rejectCircleApplication = (id: number, note: string) => {
  return request.put(`/admin/circle-application/${id}/reject`, { note })
}

// ===== 入圈申请审核（管理员） =====

export interface CircleJoinApplicationVO {
  id: number
  applicantId: number
  circleId: number
  reason: string
  status: string
  reviewerId: number | null
  reviewNote: string | null
  reviewedAt: string | null
  createTime: string
}

export const getCircleJoinApplications = (params: {
  status?: string
  circleId?: number
  page?: number
  size?: number
}) => {
  return request.get('/admin/circle-join-application/list', { params })
}

export const getCircleJoinApplicationDetail = (id: number): Promise<CircleJoinApplicationVO> => {
  return request.get(`/admin/circle-join-application/${id}`)
}

export const approveCircleJoinApplication = (id: number, note: string) => {
  return request.put(`/admin/circle-join-application/${id}/approve`, { note })
}

export const rejectCircleJoinApplication = (id: number, note: string) => {
  return request.put(`/admin/circle-join-application/${id}/reject`, { note })
}
