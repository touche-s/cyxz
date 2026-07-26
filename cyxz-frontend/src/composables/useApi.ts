import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useApi<T>(apiFn: () => Promise<T>) {
  const loading = ref(false)
  const data = ref<T | null>(null)
  const error = ref<string | null>(null)

  async function execute(): Promise<T | null> {
    loading.value = true
    error.value = null
    try {
      data.value = await apiFn()
      return data.value
    } catch (e: any) {
      const msg = e?.message || e?.msg || '请求失败'
      error.value = msg
      ElMessage.error(msg)
      return null
    } finally {
      loading.value = false
    }
  }

  return { loading, data, error, execute }
}
