import { render, screen } from '@testing-library/react'
import { AppErrorBoundary } from '../../components/AppErrorBoundary'

function BrokenComponent() {
  throw new Error('component failure')
}

describe('component layer: AppErrorBoundary', () => {
  it('renders the fallback when a child component throws', () => {
    const original = console.error
    console.error = () => undefined

    try {
      render(
        <AppErrorBoundary>
          <BrokenComponent />
        </AppErrorBoundary>,
      )

      expect(screen.getByRole('alert')).toHaveTextContent(
        'Something went wrong.',
      )
      expect(screen.getByText('Reload the page to try again.')).toBeVisible()
    } finally {
      console.error = original
    }
  })
})
