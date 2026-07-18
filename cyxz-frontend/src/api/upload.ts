import request from '@/utils/request'

/** 上传头像 */
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/** 上传帖子图片 */
export const uploadPostImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/post-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除已上传的文件 */
export const deleteUploadedFile = (url: string) => {
  return request.delete('/upload/file', { params: { url } })
}
