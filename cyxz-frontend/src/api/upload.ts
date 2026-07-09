import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  const userStore = useUserStore()
  return request.post('/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'X-User-Id': String(userStore.userInfo?.id ?? ''),
    },
  })
}

export const uploadPostImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/post-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
