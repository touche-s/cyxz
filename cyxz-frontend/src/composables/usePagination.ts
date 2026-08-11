import { ref, computed, type Ref } from 'vue'
import type { PageResult } from '@/api/types/common'

/**
 * 通用分页/滚动加载 composable
 * <p>封装页码、加载状态、是否结束、reset/loadMore 等通用逻辑，消除各列表页重复代码
 * @param fetcher 分页数据请求函数，接收 { page, size }，返回 PageResult 或数组
 * @param options 配置项
 * @param options.pageSize 每页大小，默认 10
 * @param options.onError 加载失败回调，默认不处理
 * @param options.onPageLoaded 每页加载成功后的回调，可拿到 pageResult 用于同步外部状态（如帖子评论数）
 */
export function usePagination<T>(
  fetcher: (params: { page: number; size: number }) => Promise<PageResult<T> | T[]>,
  options: {
    pageSize?: number
    onError?: (err: unknown) => void
    onPageLoaded?: (result: PageResult<T> | T[]) => void
  } = {}
) {
  const pageSize = options.pageSize ?? 10
  const list = ref<T[]>([]) as Ref<T[]>
  const page = ref(1)
  const total = ref(0)
  const loading = ref(false)
  const loadingMore = ref(false)
  const initialLoading = ref(true)

  const hasMore = computed(() => list.value.length < total.value)

  function extract(result: PageResult<T> | T[]): { records: T[]; total: number } {
    if (Array.isArray(result)) {
      return { records: result, total: result.length }
    }
    const pr = result as PageResult<T>
    const totalNum = typeof pr.total === 'string' ? Number(pr.total) : (pr.total ?? 0)
    return { records: pr.records ?? [], total: totalNum }
  }

  /**
   * 加载一页数据
   * @param reset 是否重置到第一页（清空已有数据）
   */
  async function load(reset = false) {
    if (loading.value || loadingMore.value) return
    if (reset) {
      page.value = 1
      list.value = []
    } else if (!hasMore.value && list.value.length > 0) {
      return
    }

    if (reset) loading.value = true
    else loadingMore.value = true

    try {
      const result = await fetcher({ page: page.value, size: pageSize })
      const { records, total: t } = extract(result)
      total.value = t
      if (reset) {
        list.value = records
      } else {
        list.value.push(...records)
      }
      options.onPageLoaded?.(result)
    } catch (err) {
      if (reset) list.value = []
      options.onError?.(err)
    } finally {
      loading.value = false
      loadingMore.value = false
      initialLoading.value = false
    }
  }

  /** 加载下一页 */
  function loadMore() {
    if (loading.value || loadingMore.value || !hasMore.value) return
    page.value++
    load(false)
  }

  /** 重置到第一页并加载 */
  function reset() {
    return load(true)
  }

  /** 向列表头部插入一条（发帖/发评论后立即展示） */
  function unshift(item: T) {
    list.value.unshift(item)
    total.value++
  }

  /** 按 ID 移除一条（删帖/删评论后立即更新） */
  function removeById(idField: keyof T, id: T[keyof T]) {
    const idx = list.value.findIndex(item => item[idField] === id)
    if (idx !== -1) {
      list.value.splice(idx, 1)
      total.value = Math.max(0, total.value - 1)
    }
  }

  return {
    list,
    page,
    total,
    loading,
    loadingMore,
    initialLoading,
    hasMore,
    load,
    loadMore,
    reset,
    unshift,
    removeById,
  }
}
