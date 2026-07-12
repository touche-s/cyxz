import request from '@/utils/request'

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export const uploadCover = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/cover', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const uploadPostImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/post-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
