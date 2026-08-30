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


describe('feature layer: reports', () => {
  it('requires privacy preview before report generation', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          reportType: 'FULL_DEVELOPER_REPORT',
          privateDataMode: 'EXCLUDE_PRIVATE',
          privacyScope: 'PUBLIC_ONLY',
          privateRepositoriesIncluded: false,
          privateNamesIncluded: false,
          aiAssessmentsIncluded: false,
          firstActivityAt: null,
          lastActivityAt: null,
          repositoryCount: 4,
          publicRepositoryCount: 4,
          privateRepositoryCount: 0,
          contributionCount: 10,
          reportModelVersion: 'report-v1',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    render(<ReportsView />)

    expect(
      screen.queryByRole('button', { name: 'Generate Markdown report' }),
    ).not.toBeInTheDocument()

    chooseRequiredReportSettings()

    fireEvent.click(
      screen.getByRole('button', { name: 'Preview report privacy' }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Review before generation' }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Generate Markdown report' }),
    ).toBeEnabled()

    vi.unstubAllGlobals()
  })
})
