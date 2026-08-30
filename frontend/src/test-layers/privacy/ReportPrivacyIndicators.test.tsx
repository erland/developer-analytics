import { fireEvent, render, screen } from '@testing-library/react'
import { ReportsView } from '../../components/ReportsView'

describe('privacy layer: report indicators', () => {
  it('renders the effective server-side private aggregate scope before generation', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            reportType: 'FULL_DEVELOPER_REPORT',
            privateDataMode: 'INCLUDE_PRIVATE_AGGREGATES',
            privacyScope: 'PUBLIC_PLUS_PRIVATE_AGGREGATES',
            privateRepositoriesIncluded: true,
            privateNamesIncluded: false,
            aiAssessmentsIncluded: true,
            firstActivityAt: '2025-01-01T00:00:00Z',
            lastActivityAt: '2026-08-01T00:00:00Z',
            repositoryCount: 12,
            publicRepositoryCount: 8,
            privateRepositoryCount: 4,
            contributionCount: 245,
            reportModelVersion: 'report-v1',
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    render(<ReportsView />)

    fireEvent.click(
      screen.getByRole('radio', { name: /Full developer report/ }),
    )
    fireEvent.click(
      screen.getByRole('radio', { name: /Include aggregated private data/ }),
    )
    fireEvent.click(
      screen.getByRole('radio', { name: /Hide private repository names/ }),
    )
    fireEvent.click(
      screen.getByRole('button', { name: 'Preview report privacy' }),
    )

    expect(await screen.findByText('Private scope — aggregates only')).toBeVisible()

    const preview = screen
      .getByRole('heading', { name: 'Review before generation' })
      .closest('section')
    expect(preview).not.toBeNull()
    expect(preview).toHaveTextContent('Private repositories included?Yes')
    expect(preview).toHaveTextContent('Private names included?No')
    expect(preview).toHaveTextContent('AI assessments included?Yes')

    vi.unstubAllGlobals()
  })
})
