import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'

vi.mock('./components/AuthenticatedShell', () => ({
  AuthenticatedShell: ({ user }: { user: { displayName: string } }) => (
    <main>
      <h1>Authenticated application</h1>
      <p>{user.displayName}</p>
    </main>
  ),
}))

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the anonymous landing page when there is no session', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      jsonResponse({ authenticated: false }, 401),
    )

    render(<App />)

    expect(screen.getByText(/checking your private session/i)).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', {
        name: /understand how your development work has evolved/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Sign in with GitHub' })).toHaveLength(2)
  })

  it('hands an authenticated session to the responsive application shell', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      jsonResponse({
        authenticated: true,
        provider: 'github',
        login: 'alice',
        displayName: 'Alice Example',
      }),
    )

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Authenticated application' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Alice Example')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      '/api/auth/session',
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('shows a session error state', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      jsonResponse({ message: 'failure' }, 500),
    )

    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /we could not load your session/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Session request failed with HTTP 500')).toBeInTheDocument()
  })
})
