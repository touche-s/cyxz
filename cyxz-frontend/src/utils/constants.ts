export const PAGE_SIZES = {
  DEFAULT: 12,
  POSTS: 20,
  COMMENTS: 20,
  FANS: 10,
  SEARCH: 30,
} as const

export const API_CODE = {
  SUCCESS: 200,
} as const

/** 上传文件大小限制（MB） */
export const UPLOAD_MAX_SIZE_MB = 10

/** 昵称最大长度 */
export const NICKNAME_MAX_LENGTH = 7

/** 签名最大长度 */
export const BIO_MAX_LENGTH = 50
