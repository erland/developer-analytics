import { fireEvent, render, screen } from '@testing-library/react'
import { ReportsView } from '../../components/ReportsView'


function chooseRequiredReportSettings() {
  fireEvent.click(
    screen.getByRole('radio', { name: /Full developer report/ }),
  )
  fireEvent.click(
    screen.getByRole('radio', { name: /Exclude private data/ }),
  )
  fireEvent.click(
    screen.getByRole('radio', { name: /Hide private repository names/ }),
  )
}


describe('API error-state layer: reports', () => {
  it('shows an accessible error when preview API fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(null, { status: 503 }),
      ),
    )

    render(<ReportsView />)
    chooseRequiredReportSettings()

    fireEvent.click(
      screen.getByRole('button', { name: 'Preview report privacy' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Preview failed with HTTP 503',
    )

    vi.unstubAllGlobals()
  })
})
