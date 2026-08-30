export async function setCategoryRejected(
  repositoryId: string,
  categoryKey: string,
  enabled: boolean,
) {
  await update(
    `/api/me/corrections/projects/${repositoryId}/categories/${encodeURIComponent(categoryKey)}`,
    enabled,
  )
}

export async function setTechnologySuppressed(
  technologyKey: string,
  enabled: boolean,
) {
  await update(
    `/api/me/corrections/technologies/${encodeURIComponent(technologyKey)}`,
    enabled,
  )
}

export async function setProjectExcludedFromAiProfile(
  repositoryId: string,
  enabled: boolean,
) {
  await update(
    `/api/me/corrections/projects/${repositoryId}/ai-profile`,
    enabled,
  )
}

async function update(url: string, enabled: boolean) {
  const response = await fetch(url, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ enabled }),
  })

  if (!response.ok) {
    throw new Error(`Correction update failed with HTTP ${response.status}`)
  }
}
