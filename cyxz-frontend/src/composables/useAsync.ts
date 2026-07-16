import { ref } from 'vue'

/**
 * 封装异步操作的 loading 状态
 * 用法：
 * const { loading, run } = useAsync()
 * await run(async () => { ... })
 */
export function useAsync() {
  const loading = ref(false)

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    loading.value = true
    try {
      return await fn()
    } finally {
      loading.value = false
    }
  }

  return { loading, run }
}
