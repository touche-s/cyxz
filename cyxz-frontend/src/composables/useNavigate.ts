import { useRouter } from 'vue-router'

export function useNavigate() {
  const router = useRouter()

  function to(path: string) {
    router.push(path)
  }

  function open(path: string) {
    const url = router.resolve(path).href
    window.open(url, '_blank')
  }

  function openWithQuery(path: string, query: Record<string, string>) {
    const url = router.resolve({ path, query }).href
    window.open(url, '_blank')
  }

  return { to, open, openWithQuery, router }
}
