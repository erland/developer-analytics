export type GetJsonOptions = {
  signal?: AbortSignal
  errorMessage: string
}

export async function getJson<T>(url: string, options: GetJsonOptions): Promise<T> {
  const response = await fetch(url, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    signal: options.signal,
  })

  if (!response.ok) {
    throw new Error(`${options.errorMessage} with HTTP ${response.status}`)
  }

  return await response.json() as T
}
