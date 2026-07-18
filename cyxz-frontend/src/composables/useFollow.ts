import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { followUser, unfollowUser, isFollowing } from '@/api/user'
import { useAuth } from '@/composables/useAuth'

export function useFollow() {
  const { requireLogin } = useAuth()
  const following = ref(false)
  const followLoading = ref(false)

  async function checkFollowing(targetUserId: string) {
    try {
      const res = await isFollowing(targetUserId)
      following.value = ((res.data as any).data) === true
    } catch {
      // 忽略关注状态查询失败
    }
  }

  /**
   * 切换关注状态，乐观更新 + 失败回滚
   * @param targetUserId 目标用户 ID
   * @param onSuccess 关注状态成功变更后的回调（参数为新的 following 状态）
   */
  async function toggleFollow(targetUserId: string, onSuccess?: (nowFollowing: boolean) => void) {
    if (!requireLogin()) return
    if (followLoading.value) return  // 防重复点击
    followLoading.value = true
    const oldFollowing = following.value
    following.value = !oldFollowing
    try {
      if (oldFollowing) {
        await unfollowUser(targetUserId)
        ElMessage.success('已取消关注')
      } else {
        await followUser(targetUserId)
        ElMessage.success('关注成功')
      }
      onSuccess?.(!oldFollowing)
    } catch {
      following.value = oldFollowing
      ElMessage.error('操作失败')
    } finally {
      followLoading.value = false
    }
  }

  return { following, followLoading, checkFollowing, toggleFollow }
}
