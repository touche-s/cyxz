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

export const getCircleList = () => {
  return request.get('/circle/list')
}

export const getCircleDetail = (circleId: number) => {
  return request.get(`/circle/${circleId}`)
}

export const getJoinedCircles = () => {
  return request.get('/circle/joined')
}

export const joinCircle = (circleId: number) => {
  return request.post(`/circle/${circleId}/join`)
}

export const leaveCircle = (circleId: number) => {
  return request.delete(`/circle/${circleId}/join`)
}
