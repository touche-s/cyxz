/** 默认头像 SVG 路径 */
export const DEFAULT_AVATAR = '/default-avatar.svg'

/**
 * 获取头像 URL，传入空值时返回默认头像
 * @example :src="avatarUrl(msg.senderAvatar)"
 */
export function avatarUrl(url?: string | null): string {
  return url || DEFAULT_AVATAR
}

/**
 * 判断是否在使用默认头像
 * @example v-if="!isDefaultAvatar(user.avatar)"
 */
export function isDefaultAvatar(url?: string | null): boolean {
  return !url
}
