export function getOrCreateActorId() {
  const key = 'workflow_ui_actor_id'
  try {
    const existing = localStorage.getItem(key)
    if (existing && typeof existing === 'string') return existing

    const id =
      typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : String(Date.now())

    localStorage.setItem(key, id)
    return id
  } catch {
    return null
  }
}

