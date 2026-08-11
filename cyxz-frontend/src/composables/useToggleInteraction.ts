import { ref } from 'vue'

interface ToggleConfig<T> {
  /** Field name for the boolean liked/collected state */
  likedField: string
  /** Field name for the count of likes/collections */
  countField: string
  /** API call to like/collect */
  likeApi: (id: string) => Promise<any>
  /** API call to unlike/uncollect */
  unlikeApi: (id: string) => Promise<any>
  /** Extract the ID from the target for the API call */
  idGetter: (target: T) => string
}

/**
 * Apply an optimistic like/collect toggle to a record with rollback on error.
 * Shared by single-target (useToggleInteraction) and list (createToggleAction) scenarios.
 */
async function applyToggle<T extends Record<string, any>>(target: T, config: ToggleConfig<T>) {
  const record = target as Record<string, any>
  const oldLiked = record[config.likedField]
  const oldCount = record[config.countField]

  record[config.likedField] = !oldLiked
  record[config.countField] = oldLiked ? Math.max(oldCount - 1, 0) : oldCount + 1

  try {
    if (oldLiked) {
      await config.unlikeApi(config.idGetter(target))
    } else {
      await config.likeApi(config.idGetter(target))
    }
  } catch {
    record[config.likedField] = oldLiked
    record[config.countField] = oldCount
  }
}

/**
 * Single-target interaction hook with reactive loading/popping state.
 * Suitable for detail pages where one post/comment is bound to the view
 * (e.g. PostDetail.vue action bar).
 */
export function useToggleInteraction<T extends Record<string, any>>(options: {
  target: () => T | null
} & ToggleConfig<T>) {
  const loading = ref(false)
  const popping = ref(false)

  async function toggle() {
    const t = options.target()
    if (!t || loading.value) return

    loading.value = true
    popping.value = true
    setTimeout(() => { popping.value = false }, 450)

    try {
      await applyToggle(t, options)
    } finally {
      loading.value = false
    }
  }

  return { toggle, loading, popping }
}

/**
 * Factory for a list-item toggle action.
 * Returns a function accepting the item as an argument — suitable for v-for
 * lists (e.g. Discover/CircleDetail card grids) where each item manages its
 * own visual feedback (PostCard handles the popping animation internally).
 */
export function createToggleAction<T extends Record<string, any>>(config: ToggleConfig<T>) {
  return function toggle(item: T) {
    return applyToggle(item, config)
  }
}
