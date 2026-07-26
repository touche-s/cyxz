import { ref } from 'vue'

export function useToggleInteraction<T extends Record<string, any>>(options: {
  /** Getter for the target reactive object (post/comment) */
  target: () => T | null,
  /** Field name for the boolean liked/collected state */
  likedField: string,
  /** Field name for the count of likes/collections */
  countField: string,
  /** API call to like/collect */
  likeApi: (id: string) => Promise<any>,
  /** API call to unlike/uncollect */
  unlikeApi: (id: string) => Promise<any>,
  /** Extract the ID from the target for the API call */
  idGetter: (target: T) => string,
}) {
  const loading = ref(false)
  const popping = ref(false)

  async function toggle() {
    const t = options.target()
    if (!t || loading.value) return

    const oldLiked = t[options.likedField]
    const oldCount = t[options.countField]

    loading.value = true
    t[options.likedField] = !oldLiked
    t[options.countField] = oldLiked ? Math.max(oldCount - 1, 0) : oldCount + 1
    popping.value = true
    setTimeout(() => { popping.value = false }, 450)

    try {
      if (oldLiked) {
        await options.unlikeApi(options.idGetter(t))
      } else {
        await options.likeApi(options.idGetter(t))
      }
    } catch {
      t[options.likedField] = oldLiked
      t[options.countField] = oldCount
    } finally {
      loading.value = false
    }
  }

  return { toggle, loading, popping }
}
