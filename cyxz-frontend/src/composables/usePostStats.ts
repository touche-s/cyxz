import { ref } from 'vue'
import { getPostStats, getUserPostStats } from '@/api/post'
import type { PostStatsVO } from '@/api/post'

/**
 * 帖子统计数据加载 composable
 * <p>统一"我的统计"（创作中心）与"指定用户统计"（个人空间）两种加载逻辑。
 * stats 初始为 null，调用方按需用 ?. + ?? 0 兜底。
 */
export function usePostStats() {
  const stats = ref<PostStatsVO | null>(null)
  const loading = ref(false)

  /** 加载当前登录用户的帖子统计（创作中心数据概览） */
  async function loadMyStats() {
    loading.value = true
    try {
      stats.value = await getPostStats()
    } catch (error) {
      console.error('加载数据统计失败:', error)
    } finally {
      loading.value = false
    }
  }

  /** 加载指定用户的帖子统计（个人空间获赞/浏览） */
  async function loadUserStats(userId: string) {
    try {
      const data = await getUserPostStats(userId)
      if (data) {
        stats.value = {
          totalPosts: Number(data.totalPosts) || 0,
          totalViews: Number(data.totalViews) || 0,
          totalLikes: Number(data.totalLikes) || 0,
          totalCollections: Number(data.totalCollections) || 0,
        }
      }
    } catch {
      // 忽略统计加载失败，保留 null 由模板兜底为 0
    }
  }

  return { stats, loading, loadMyStats, loadUserStats }
}
