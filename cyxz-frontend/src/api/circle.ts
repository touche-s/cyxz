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
