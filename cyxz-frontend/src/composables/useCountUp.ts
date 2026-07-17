import { ref, onUnmounted } from 'vue'

export function useCountUp(
  target: () => number,
  duration = 1200,
) {
  const displayValue = ref(0)
  let rafId = 0
  let observer: IntersectionObserver | null = null

  function animate() {
    const end = target()
    if (end <= 0) {
      displayValue.value = end
      return
    }
    const startTime = performance.now()

    function step(now: number) {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      displayValue.value = Math.round(eased * end)
      if (progress < 1) {
        rafId = requestAnimationFrame(step)
      }
    }

    rafId = requestAnimationFrame(step)
  }

  function startObserve(el: Element) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            animate()
            observer?.unobserve(el)
          }
        })
      },
      { threshold: 0.3 },
    )
    observer.observe(el)
  }

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    observer?.disconnect()
  })

  return {
    displayValue,
    startObserve,
    animate,
  }
}
