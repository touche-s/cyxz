import { ref } from 'vue'
import { ElMessage } from 'element-plus'

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
        const msg = e?.response?.data?.msg || e?.message || '请求失败'
        ElMessage.error(msg)
      }
      return undefined
    } finally {
      loading.value = false
    }
  }

  return { loading, run }
}
