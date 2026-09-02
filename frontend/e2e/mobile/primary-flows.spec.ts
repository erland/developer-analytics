import { expect, test, type Page, type Route } from '@playwright/test'

let authenticated = false

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })

async function installApiFixtures(page: Page) {
  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()

    if (path === '/api/auth/session') {
      if (!authenticated) return json(route, {}, 401)
      return json(route, {
        authenticated: true,
        provider: 'github',
        login: 'mobile-developer',
        displayName: 'Mobile Developer',
      })
    }

    if (path === '/api/me/sync-runs') return json(route, [])

    if (path === '/api/me/repositories') return json(route, [
      {
        id: 'repo-1',
        name: 'mobile-dashboard',
        fullName: 'mobile-developer/mobile-dashboard',
        htmlUrl: 'https://example.invalid/mobile-dashboard',
        visibility: 'PUBLIC',
        ownershipRelation: 'OWNED_BY_USER',
        isFork: false,
        isArchived: false,
        firstActivityAt: '2025-01-01T00:00:00Z',
        lastActivityAt: '2026-08-01T00:00:00Z',
      },
    ])
    if (path === '/api/me/technology-assessments') return json(route, [])
    if (path === '/api/me/significant-external-projects') return json(route, [])
    if (path === '/api/me/technology-timeline') return json(route, [])
    if (/\/api\/me\/repositories\/[^/]+\/project-categories$/.test(path)) return json(route, [])
    if (/\/api\/me\/repositories\/[^/]+\/contributions$/.test(path)) return json(route, [])

    if (path === '/api/me/activity') return json(route, {
      commitCount: 126,
      activeProjects: 12,
      averageCommitSize: 34,
      medianCommitSize: 18,
      additions: 3100,
      deletions: 1400,
      firstActivityAt: '2025-01-01T00:00:00Z',
      lastActivityAt: '2026-08-01T00:00:00Z',
      commitsPerYear: [
        { year: 2025, commits: 46 },
        { year: 2026, commits: 80 },
      ],
      commitsPerMonth: [
        { month: '2026-06', commits: 18, activeProjects: 4 },
        { month: '2026-07', commits: 27, activeProjects: 6 },
        { month: '2026-08', commits: 35, activeProjects: 7 },
      ],
    })

    if (path === '/api/me/project-inventory') return json(route, {
      items: [
        {
          id: 'repo-1',
          name: 'mobile-dashboard',
          description: 'Responsive analytics UI',
          htmlUrl: 'https://example.invalid/mobile-dashboard',
          ownershipRelation: 'OWNED_BY_USER',
          visibility: 'PUBLIC',
          lastActivityAt: '2026-08-01T00:00:00Z',
          categories: [{ key: 'web-app', name: 'Web application' }],
          technologies: [{ key: 'react', name: 'React' }],
        },
      ],
      total: 126,
      page: Number(url.searchParams.get('page') ?? 0),
      pageSize: 25,
      totalPages: 6,
    })

    if (path === '/api/me/technologies') return json(route, [
      {
        technologyKey: 'react',
        technologyName: 'React',
        technologyCategory: 'Frontend',
        evidenceLevel: 'STRONG',
        evidenceScore: 92,
        projectCount: 18,
        evidenceCount: 43,
        independentEvidenceTypes: 3,
        firstObservedAt: '2022-01-01T00:00:00Z',
        lastObservedAt: '2026-08-01T00:00:00Z',
        recentProjectCount: 8,
        privacyProvenance: 'PUBLIC_ONLY',
        rationale: {},
        timeline: [
          { month: '2026-07', projectCount: 6, activityCount: 18 },
          { month: '2026-08', projectCount: 7, activityCount: 24 },
        ],
        representativeProjects: [
          {
            repositoryId: 'repo-1',
            repositoryName: 'mobile-dashboard',
            htmlUrl: 'https://example.invalid/mobile-dashboard',
            visibility: 'PUBLIC',
            ownershipRelation: 'OWNED_BY_USER',
            lastActivityAt: '2026-08-01T00:00:00Z',
            evidenceCount: 8,
          },
        ],
      },
    ])

    if (path === '/api/me/project-types') return json(route, [])

    if (path === '/api/me/ai/status') return json(route, {
      configured: true,
      providerId: 'gemini',
      message: 'AI-assisted analysis is configured.',
    })
    if (path === '/api/me/ai/privacy') return json(route, { policy: 'PUBLIC_ONLY' })
    if (path === '/api/me/ai/insights') return json(route, {
      status: 'AVAILABLE',
      aiGenerated: true,
      likelyRoles: [
        { role: 'Full-stack developer', confidence: 0.88, rationale: 'Evidence across frontend and backend.' },
      ],
      technicalFocus: 'Web platforms and developer tooling',
      breadthDepthObservation: 'Broad evidence with recurring React depth',
      technologyEvolutionSummary: 'Increasing TypeScript usage',
      openSourceEngagementSummary: 'Regular public contributions',
      analysisVersion: 'v1',
      providerId: 'gemini',
      modelId: 'mobile-fixture',
      privacyProvenance: 'PUBLIC_ONLY',
      createdAt: '2026-08-30T12:00:00Z',
    })

    if (path === '/api/me/data-sources/github') return json(route, {
      privateRepositoriesAuthorised: true,
      privateRepositoriesAuthorisedAt: '2026-08-01T00:00:00Z',
    })
    if (path === '/api/me/private-repositories') return json(route, [
      {
        id: 'private-1',
        name: 'private-mobile',
        fullName: 'mobile-developer/private-mobile',
        htmlUrl: null,
        includedInAnalysis: true,
        syncStatus: 'SYNCED',
      },
    ])
    if (path === '/api/me/external-clients') return json(route, [])

    if (path === '/api/me/reports/preview' && method === 'POST') return json(route, {
      reportType: 'PUBLIC_OSS_REPORT',
      privateDataMode: 'EXCLUDE_PRIVATE',
      privacyScope: 'PUBLIC_ONLY',
      privateRepositoriesIncluded: false,
      privateNamesIncluded: false,
      aiAssessmentsIncluded: true,
      firstActivityAt: '2025-01-01T00:00:00Z',
      lastActivityAt: '2026-08-01T00:00:00Z',
      repositoryCount: 126,
      publicRepositoryCount: 120,
      privateRepositoryCount: 0,
      contributionCount: 540,
      reportModelVersion: 'report-v1',
    })

    if (path.startsWith('/api/me/')) return json(route, {})
    return json(route, {}, 404)
  })
}

async function openSection(page: Page, name: string) {
  const toggle = page.getByRole('button', { name: 'Toggle navigation' })
  await toggle.click()
  await expect(toggle).toHaveAttribute('aria-expanded', 'true')
  await page.getByRole('button', { name, exact: true }).click()
  await expect(page.getByRole('heading', { name, level: 1 })).toBeVisible()
  await expect(toggle).toHaveAttribute('aria-expanded', 'false')
}

async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    innerWidth: window.innerWidth,
  }))
  expect(
    overflow.scrollWidth,
    `mobile page overflowed horizontally: ${overflow.scrollWidth}px > ${overflow.innerWidth}px`,
  ).toBeLessThanOrEqual(overflow.innerWidth + 1)
}

test.beforeEach(async ({ page }) => {
  authenticated = false
  await installApiFixtures(page)
})

test('primary v1 flows remain usable at phone width without wide-table dependency', async ({ page }) => {
  await page.goto('/')

  const signIn = page.getByRole('link', { name: 'Sign in with GitHub' }).first()
  await expect(signIn).toBeVisible()
  await expect(signIn).toHaveAttribute('href', '/api/auth/github/login')
  await expectNoHorizontalOverflow(page)

  authenticated = true
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Overview', level: 1 })).toBeVisible()
  await expect(page.getByText('Mobile Developer', { exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Activity')
  await expect(page.locator('.bar-chart').first()).toBeVisible()
  await expect(page.getByText('126').first()).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Projects')
  await expect(page.locator('.inventory-filters')).toBeVisible()
  await expect(page.getByRole('textbox').first()).toBeVisible()
  await expect(page.getByText('mobile-dashboard').first()).toBeVisible()
  await expect(page.locator('table')).toHaveCount(0)
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Technologies')
  await expect(page.getByRole('button', { name: /React/ })).toBeVisible()
  await expect(page.getByText('STRONG', { exact: true }).first()).toBeVisible()
  await expect(page.locator('table')).toHaveCount(0)
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'AI insights')
  await expect(page.getByText('Full-stack developer')).toBeVisible()
  await expect(page.getByText('Public data only')).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Reports')
  await page.getByText('Public OSS report', { exact: true }).click()
  await page.getByText('Exclude private data', { exact: true }).click()
  await page.getByText('Hide private repository names', { exact: true }).click()
  const preview = page.getByRole('button', { name: 'Preview report privacy' })
  await expect(preview).toBeEnabled()
  await preview.click()
  await expect(page.getByRole('heading', { name: 'Review before generation' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Privacy/data sources')
  await expect(page.getByRole('heading', { name: 'Repository access' })).toBeVisible()
  await expect(page.getByText(/To include a private repository in Developer Analytics/i)).toBeVisible()
  await expect(page.getByRole('button', { name: 'Recover interrupted jobs' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Account')
  await expect(page.getByText('GPT/API access tokens')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Create external client token' })).toBeVisible()
  await expect(page.getByText('Type DELETE_MY_DATA to confirm')).toBeVisible()
  await expect(page.locator('table')).toHaveCount(0)
  await expectNoHorizontalOverflow(page)
})
