import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'

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

  it('renders the authenticated responsive application shell', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)

      if (url.includes('/api/auth/session')) {
        return jsonResponse({
          authenticated: true,
          provider: 'github',
          login: 'alice',
          displayName: 'Alice Example',
        })
      }

      if (url.includes('/api/me/sync-runs')) {
        return jsonResponse([])
      }
      if (url === '/api/me/repositories') {
        return jsonResponse([
          {
            id: 'repo-1',
            visibility: 'PUBLIC',
            ownershipRelation: 'OWNED_BY_USER',
            lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
          },
          {
            id: 'repo-2',
            visibility: 'PRIVATE',
            ownershipRelation: 'ORGANIZATION_OWNED',
            lastActivityAt: '2026-07-15T08:00:00Z',
          },
        ])
      }
      if (url === '/api/me/private-repositories') {
        return jsonResponse([
          { id: 'private-1', name: 'private-app', fullName: 'alice/private-app', htmlUrl: null, includedInAnalysis: true, syncStatus: 'SYNCED' },
        ])
      }
      if (url === '/api/me/private-repositories/refresh') {
        return jsonResponse({ jobId: 'job-1' })
      }
      if (url.startsWith('/api/me/corrections/')) {
        return Promise.resolve(new Response(null, { status: 204 }))
      }

if (url === '/api/me/reports/preview') {
  return jsonResponse({
    reportType: 'FULL_DEVELOPER_REPORT',
    privateDataMode: 'EXCLUDE_PRIVATE',
    privacyScope: 'PUBLIC_ONLY',
    privateRepositoriesIncluded: false,
    privateNamesIncluded: false,
    aiAssessmentsIncluded: false,
    firstActivityAt: '2025-01-01T00:00:00Z',
    lastActivityAt: '2026-08-30T00:00:00Z',
    repositoryCount: 12,
    publicRepositoryCount: 12,
    privateRepositoryCount: 0,
    contributionCount: 1000,
    reportModelVersion: 'report-v1',
  })
}

      if (url === '/api/me/external-clients') {
        return jsonResponse([])
      }
      if (url === '/api/me/ai/insights') {
        return jsonResponse({
          status: 'REUSED',
          aiGenerated: true,
          likelyRoles: [
            {
              role: 'Backend developer',
              confidence: 0.82,
              rationale: 'Strong backend and API signals',
            },
          ],
          technicalFocus: 'Backend services and developer tooling.',
          breadthDepthObservation: 'Broad technology exposure with deeper backend evidence.',
          technologyEvolutionSummary: 'Recent activity increasingly includes container tooling.',
          openSourceEngagementSummary: 'Regular activity across owned and external repositories.',
          analysisVersion: 'user-ai-v1',
          providerId: 'gemini',
          modelId: 'test-model',
          privacyProvenance: 'PUBLIC_ONLY',
          createdAt: '2026-08-30T09:00:00Z',
        })
      }
      if (url === '/api/me/ai/privacy') {
        return jsonResponse({
          policy: 'PRIVATE_AI_DISABLED',
          privateMetadataAllowed: false,
          privateContentAllowed: false,
        })
      }
      if (url === '/api/me/ai/status') {
        return jsonResponse({
          configured: false,
          providerId: 'disabled',
          message: 'AI is not configured. Deterministic analytics remain fully available.',
        })
      }
      if (url === '/api/me/data-sources/github') {
        return jsonResponse({
          status: 'CONNECTED',
          privateRepositoriesAuthorised: true,
          privateRepositoriesAuthorisedAt: '2026-08-30T08:00:00Z',
        })
      }
      if (url === '/api/me/project-types') {
        return jsonResponse([
          {
            categoryKey: 'backend-service',
            categoryName: 'Backend service',
            projectCount: 4,
            activityCount: 120,
            timeline: [
              {
                month: '2026-08',
                activityCount: 30,
                activeProjectCount: 3,
              },
            ],
            representativeProjects: [
              {
                repositoryId: 'repo-1',
                repositoryName: 'demo-service',
                htmlUrl: 'https://github.com/alice/demo-service',
                visibility: 'PUBLIC',
                ownershipRelation: 'OWNED_BY_USER',
                lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
                contributionCount: 42,
              },
            ],
          },
        ])
      }
      if (url === '/api/me/technologies') {
        return jsonResponse([
          {
            technologyKey: 'java',
            technologyName: 'Java',
            technologyCategory: 'LANGUAGE',
            evidenceLevel: 'STRONG',
            evidenceScore: 88,
            projectCount: 6,
            evidenceCount: 14,
            independentEvidenceTypes: 3,
            firstObservedAt: '2024-01-01T00:00:00Z',
            lastObservedAt: '2026-08-20T08:00:00Z',
            recentProjectCount: 4,
            privacyProvenance: 'INCLUDES_PRIVATE',
            rationale: { score: 88 },
            timeline: [
              { month: '2026-08-01', projectCount: 3, activityCount: 12 },
            ],
            representativeProjects: [
              {
                repositoryId: 'repo-1',
                repositoryName: 'demo-service',
                htmlUrl: 'https://github.com/alice/demo-service',
                visibility: 'PUBLIC',
                ownershipRelation: 'OWNED_BY_USER',
                lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
                evidenceCount: 3,
              },
            ],
          },
        ])
      }
      if (url === '/api/me/technology-assessments') {
        return jsonResponse([
          {
            technologyKey: 'java',
            technologyName: 'Java',
            strength: 'STRONG',
            score: 88,
          },
        ])
      }
      if (url === '/api/me/significant-external-projects') {
        return jsonResponse([
          {
            repositoryId: 'repo-2',
            repositoryName: 'external-project',
            matchReason: 'BOTH',
            significanceScore: 80,
            involvementScore: 72,
          },
        ])
      }
      if (url === '/api/me/technology-timeline') {
        return jsonResponse([
          {
            technologyKey: 'java',
            firstObservedAt: '2024-01-01T00:00:00Z',
            lastObservedAt: '2026-08-20T08:00:00Z',
          },
        ])
      }
      if (url.includes('/project-categories')) {
        return jsonResponse([
          {
            categoryKey: 'backend-service',
            categoryName: 'Backend service',
            confidence: 'HIGH',
          },
        ])
      }
      if (url.includes('/contributions')) {
        return jsonResponse([
          {
            type: 'COMMIT',
            occurredAt: '2026-08-20T08:00:00Z',
          },
        ])
      }
      if (url === '/api/me/projects/repo-1') {
        return jsonResponse({
          metadata: {
            id: 'repo-1',
            provider: 'github',
            name: 'demo-service',
            fullName: 'alice/demo-service',
            description: 'Example backend service',
            htmlUrl: 'https://github.com/alice/demo-service',
            visibility: 'PUBLIC',
            ownershipRelation: 'OWNED_BY_USER',
            ownerLogin: 'alice',
            fork: false,
            archived: false,
            topics: ['api', 'quarkus'],
            lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
          },
          activity: {
            commits: 42,
            pullRequests: 4,
            reviews: 3,
            issues: 2,
            additions: 500,
            deletions: 210,
            firstActivityAt: '2026-01-01T08:00:00Z',
            lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
            timeline: [{ month: '2026-08', commits: 12 }],
          },
          technologies: [
            {
              technologyKey: 'quarkus',
              technologyName: 'Quarkus',
              evidenceType: 'MANIFEST',
              strength: 'OBSERVED',
              sourceValue: 'pom.xml:io.quarkus',
              measuredValue: null,
              observedAt: '2026-08-20T08:00:00Z',
              privacyProvenance: 'PUBLIC_ONLY',
              rejectedByUser: false,
            },
          ],
          categories: [
            {
              categoryKey: 'backend-service',
              categoryName: 'Backend service',
              source: 'DETERMINISTIC',
              confidence: 'HIGH',
              rationale: { score: 9 },
              privacyProvenance: 'PUBLIC_ONLY',
            },
          ],
          assessment: {
            significanceLevel: 'HIGH',
            significanceScore: 72,
            significanceRationale: { activityScore: 20 },
            involvementLevel: 'VERY_HIGH',
            involvementScore: 84,
            involvementRationale: { contributionScore: 35 },
            calculatedAt: '2026-08-20T08:00:00Z',
            privacyProvenance: 'PUBLIC_ONLY',
          },
          synchronisation: {
            status: 'SYNCED',
            lastSeenAt: '2026-08-20T08:00:00Z',
            error: null,
          },
        })
      }
      if (url.startsWith('/api/me/project-inventory')) {
        return jsonResponse({
          items: [
            {
              id: 'repo-1',
              name: 'demo-service',
              description: 'Example backend service',
              htmlUrl: 'https://github.com/alice/demo-service',
              ownershipRelation: 'OWNED_BY_USER',
              visibility: 'PUBLIC',
              lastActivityAt: '2026-08-20T08:00:00Z',
            excludedFromAiProfile: false,
              categories: [
                { key: 'backend-service', name: 'Backend service' },
              ],
              technologies: [
                { key: 'java', name: 'Java' },
                { key: 'quarkus', name: 'Quarkus' },
              ],
            },
          ],
          total: 1,
          page: 0,
          pageSize: 25,
          totalPages: 1,
        })
      }
      if (url.startsWith('/api/me/activity')) {
        return jsonResponse({
          commitCount: 42,
          activeProjects: 6,
          averageCommitSize: 18.5,
          medianCommitSize: 11,
          additions: 500,
          deletions: 210,
          firstActivityAt: '2026-01-01T08:00:00Z',
          lastActivityAt: '2026-08-20T08:00:00Z',
          commitsPerYear: [{ year: 2026, commits: 42 }],
          commitsPerMonth: [
            { month: '2026-08', commits: 12, activeProjects: 3 },
          ],
        })
      }

      return jsonResponse({}, 404)
    })

    render(<App />)

    expect(await screen.findByText('Alice Example')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Overview' })).toBeInTheDocument()

    expect(await screen.findByText('Repositories analysed')).toBeInTheDocument()
    expect(screen.getByText('Own / external')).toBeInTheDocument()
    expect(screen.getByText('Public / private')).toBeInTheDocument()
    expect(screen.getByText('Commits observed')).toBeInTheDocument()
    expect(screen.getByText('Java')).toBeInTheDocument()
    expect(screen.getByText('Backend service')).toBeInTheDocument()
    expect(screen.getByText('external-project')).toBeInTheDocument()

    for (const section of [
      'Overview',
      'Activity',
      'Projects',
      'Technologies',
      'Project types',
      'Contributions',
      'AI insights',
      'Reports',
      'Privacy/data sources',
      'Account',
    ]) {
      expect(screen.getByRole('button', { name: section })).toBeInTheDocument()
    }

    fireEvent.click(screen.getByRole('button', { name: 'Activity' }))
    expect(screen.getByRole('heading', { name: 'Activity', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText('Changed lines per month')).toBeInTheDocument()
    expect(screen.getByText('Average commit size')).toBeInTheDocument()
    expect(screen.getByText('+500')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Projects' }))
    expect(screen.getByRole('heading', { name: 'Projects', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText('demo-service')).toBeInTheDocument()
    expect(screen.getAllByText('Quarkus').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByLabelText('Project filters')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'demo-service' }))
    expect(await screen.findByText('Project detail')).toBeInTheDocument()
    expect(screen.getByText('Project significance')).toBeInTheDocument()
    expect(screen.getByText('User involvement')).toBeInTheDocument()
    expect(screen.getByText('SYNCED')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Technologies' }))
    expect(screen.getByRole('heading', { name: 'Technologies', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText('Technology evidence')).toBeInTheDocument()
    expect(screen.getByText('Representative projects')).toBeInTheDocument()
    expect(screen.getByText('evidence score')).toBeInTheDocument()


    fireEvent.click(screen.getByRole('button', { name: 'AI insights' }))
    expect(
      screen.getByRole('heading', { name: 'AI insights', level: 1 }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Deterministic analytics remain primary'),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('User-level insights'),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('AI-generated interpretation', { exact: false }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole('radio', { name: /Disable AI for private data/ }),
    ).toBeChecked()

    fireEvent.click(screen.getByRole('button', { name: 'Account' }))
    expect(
      screen.getByRole('heading', { name: 'Account', level: 1 }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('GPT/API access tokens'),
    ).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Reports' }))
    expect(
      screen.getByRole('heading', { name: 'Reports', level: 1 }),
    ).toBeInTheDocument()
    const previewButton = screen.getByRole('button', {
      name: 'Preview report privacy',
    })
    expect(previewButton).toBeDisabled()

    fireEvent.click(
      screen.getByRole('radio', { name: /Full developer report/ }),
    )
    fireEvent.click(
      screen.getByRole('radio', { name: /Exclude private data/ }),
    )
    fireEvent.click(
      screen.getByRole('radio', { name: /Hide private repository names/ }),
    )
    expect(previewButton).toBeEnabled()

    fireEvent.click(previewButton)
    expect(
      await screen.findByRole('button', { name: 'Generate Markdown report' }),
    ).toBeEnabled()

    fireEvent.click(screen.getByRole('button', { name: 'Privacy/data sources' }))
    expect(
      screen.getByRole('heading', { name: 'Privacy/data sources', level: 1 }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Private repositories'),
    ).toBeInTheDocument()
    expect(screen.getByText('Managed by your GitHub App installation')).toBeInTheDocument()
    expect(screen.getByText(/install the GitHub App for that repository/i)).toBeInTheDocument()
    expect(screen.queryByLabelText('Include in analysis')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Project types' }))
    expect(screen.getByRole('heading', { name: 'Project types', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText('Project category')).toBeInTheDocument()
    expect(screen.getByText('Category activity over time')).toBeInTheDocument()
    expect(screen.getAllByText('120').length).toBeGreaterThanOrEqual(2)

    await waitFor(() =>
      expect(screen.getAllByText('No completed sync yet').length).toBeGreaterThan(0),
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
  })
})
