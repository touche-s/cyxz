import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/utils/request'
import { ErrorCode } from '@/utils/errorCode'

export function useApi() {
  const loading = ref(false)

  async function run<T>(
    fn: () => Promise<T>,
    options?: {
      onError?: (e: any) => void
      silent?: boolean
    },
  ): Promise<T | undefined> {
    loading.value = true
    try {
      return await fn()
    } catch (e: any) {
      if (options?.onError) {
        options.onError(e)
      } else if (!options?.silent) {
        const msg = e?.response?.data?.message || e?.message || '请求失败'
        // 内容违规：用更长持续时间的错误提示，让用户明确感知违规
        if (e instanceof ApiError && e.code === ErrorCode.CONTENT_SENSITIVE) {
          ElMessage.error({ message: msg, duration: 5000 })
        } else {
          ElMessage.error(msg)
        }
      }
      return undefined
    } finally {
      loading.value = false
    }
  }

  return { loading, run }
}
