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
      commitSizeStatisticsAvailable: true,
      lineStatisticsCommitCount: 126,
      firstActivityAt: '2025-01-01T00:00:00Z',
      lastActivityAt: '2026-08-16T00:00:00Z',
      commitsPerYear: [
        { year: 2025, commits: 46, additions: 900, deletions: 400, changedLines: 1300, lineStatisticsCommitCount: 46, activeProjects: 5, projects: ['repo-1'] },
        { year: 2026, commits: 80, additions: 2200, deletions: 1000, changedLines: 3200, lineStatisticsCommitCount: 80, activeProjects: 7, projects: ['repo-1'] },
      ],
      commitsPerMonth: [
        { month: '2026-06', commits: 18, additions: 400, deletions: 180, changedLines: 580, lineStatisticsCommitCount: 18, activeProjects: 4, projects: ['repo-1'] },
        { month: '2026-07', commits: 27, additions: 700, deletions: 300, changedLines: 1000, lineStatisticsCommitCount: 27, activeProjects: 6, projects: ['repo-1'] },
        { month: '2026-08', commits: 35, additions: 1100, deletions: 520, changedLines: 1620, lineStatisticsCommitCount: 35, activeProjects: 7, projects: ['repo-1'] },
      ],
      commitsPerWeek: [
        { week: '2026-08-03', commits: 15, additions: 480, deletions: 220, changedLines: 700, lineStatisticsCommitCount: 15, activeProjects: 1, projects: ['repo-1'] },
        { week: '2026-08-10', commits: 20, additions: 620, deletions: 300, changedLines: 920, lineStatisticsCommitCount: 20, activeProjects: 1, projects: ['repo-1'] },
      ],
      projectsOverTime: [
        {
          repositoryId: 'repo-1',
          repositoryName: 'mobile-dashboard',
          firstActivityAt: '2025-01-01T00:00:00Z',
          lastActivityAt: '2026-08-16T00:00:00Z',
          commits: 126,
          projectType: 'Web application',
          technology: 'React',
          projectTypes: ['Web application'],
          technologies: ['React', 'TypeScript'],
          monthlyActivity: [
            { period: '2025-05', commits: 46, additions: 900, deletions: 400, changedLines: 1300, lineStatisticsCommitCount: 46 },
            { period: '2026-06', commits: 18, additions: 400, deletions: 180, changedLines: 580, lineStatisticsCommitCount: 18 },
            { period: '2026-07', commits: 27, additions: 700, deletions: 300, changedLines: 1000, lineStatisticsCommitCount: 27 },
            { period: '2026-08', commits: 35, additions: 1100, deletions: 520, changedLines: 1620, lineStatisticsCommitCount: 35 },
          ],
          weeklyActivity: [
            { period: '2026-08-03', parentMonth: '2026-08', commits: 15, additions: 480, deletions: 220, changedLines: 700, lineStatisticsCommitCount: 15 },
            { period: '2026-08-10', parentMonth: '2026-08', commits: 20, additions: 620, deletions: 300, changedLines: 920, lineStatisticsCommitCount: 20 },
          ],
        },
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
      facets: {
        technologies: [
          { key: 'react', name: 'React', count: 18 },
          { key: 'typescript', name: 'TypeScript', count: 14 },
        ],
        projectTypes: [
          { key: 'web-app', name: 'Web application', count: 12 },
        ],
        ownership: [
          { key: 'own', name: 'Own', count: 100 },
          { key: 'external', name: 'External', count: 26 },
        ],
      },
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
          { month: '2026-07', projectCount: 6, commits: 18, changedLines: 1000, lineStatisticsCommitCount: 18 },
          { month: '2026-08', projectCount: 7, commits: 24, changedLines: 1620, lineStatisticsCommitCount: 24 },
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
  await expect(page.getByRole('heading', { name: 'Changed lines over time' })).toBeVisible()
  await expect(page.getByRole('button', { name: /2026/ })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Activity statistics' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Projects')
  await expect(page.locator('.project-scope-filters')).toBeVisible()
  await expect(page.getByRole('textbox').first()).toBeVisible()
  await expect(page.getByText('mobile-dashboard').first()).toBeVisible()
  await expect(page.locator('table')).toHaveCount(0)
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Technologies')
  await expect(page.getByRole('button', { name: 'Remove Technology: React' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Edit filters' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Activity in projects using React' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Projects matching this selection' })).toBeVisible()
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


test('Explore analysis keeps filters compact and primary content reachable on phone width', async ({ page }) => {
  authenticated = true
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Overview', level: 1 })).toBeVisible()

  await openSection(page, 'Technologies')

  const editFilters = page.getByRole('button', { name: 'Edit filters' })
  await expect(editFilters).toBeVisible()
  await expect(editFilters).toHaveAttribute('aria-expanded', 'false')
  await expect(page.getByRole('button', { name: 'Remove Technology: React' })).toBeVisible()

  const technologySelect = page.getByRole('combobox', { name: 'Technology' })
  await expect(technologySelect).not.toBeVisible()
  await editFilters.click()
  const doneFilters = page.getByRole('button', { name: 'Done' })
  await expect(doneFilters).toHaveAttribute('aria-expanded', 'true')
  await expect(technologySelect).toBeVisible()
  await expect(doneFilters).toBeVisible()
  await page.getByRole('button', { name: 'Done' }).click()
  await expect(technologySelect).not.toBeVisible()

  const overTime = page.getByRole('heading', { name: 'Activity in projects using React' })
  const matchingProjects = page.getByRole('heading', { name: 'Projects matching this selection' })
  await expect(overTime).toBeVisible()
  await expect(matchingProjects).toBeVisible()
  await expect(page.getByText('mobile-dashboard', { exact: true }).first()).toBeVisible()
  const order = await page.evaluate(() => {
    const over = Array.from(document.querySelectorAll('h2')).find(node => node.textContent?.includes('Activity in projects using React'))
    const projects = Array.from(document.querySelectorAll('h2')).find(node => node.textContent === 'Projects matching this selection')
    const evidence = Array.from(document.querySelectorAll('summary')).find(node => node.textContent?.includes('Evidence and statistics'))
    return {
      overTop: over?.getBoundingClientRect().top ?? Number.MAX_SAFE_INTEGER,
      projectsTop: projects?.getBoundingClientRect().top ?? Number.MAX_SAFE_INTEGER,
      evidenceTop: evidence?.getBoundingClientRect().top ?? Number.MAX_SAFE_INTEGER,
    }
  })
  expect(order.overTop).toBeLessThan(order.projectsTop)
  expect(order.projectsTop).toBeLessThan(order.evidenceTop)
  await expectNoHorizontalOverflow(page)

  await openSection(page, 'Activity')
  const timelineHeading = page.getByRole('heading', { name: 'Changed lines over time' })
  const statsHeading = page.getByRole('heading', { name: 'Activity statistics' })
  await expect(timelineHeading).toBeVisible()
  await expect(statsHeading).toBeVisible()
  const timelineBeforeStats = await page.evaluate(() => {
    const timeline = Array.from(document.querySelectorAll('h2')).find(node => node.textContent === 'Changed lines over time')
    const stats = Array.from(document.querySelectorAll('h2')).find(node => node.textContent === 'Activity statistics')
    return Boolean(timeline && stats && (timeline.compareDocumentPosition(stats) & Node.DOCUMENT_POSITION_FOLLOWING))
  })
  expect(timelineBeforeStats).toBe(true)

  await page.getByRole('button', { name: /2026/ }).click()
  await expect(page.getByRole('button', { name: /August 2026/ })).toBeVisible()
  await page.getByRole('button', { name: /August 2026/ }).click()
  await expect(page.getByRole('button', { name: /Week of Aug 3/ })).toBeVisible()
  await page.getByRole('button', { name: /Week of Aug 3/ }).click()
  await expect(page.getByText('Projects during this week')).toBeVisible()
  await expect(page.getByRole('button', { name: 'mobile-dashboard' })).toBeVisible()

  await page.getByRole('button', { name: '← Back to 2026' }).click()
  await expect(page.getByRole('button', { name: /August 2026/ })).toBeVisible()
  await page.getByRole('button', { name: '← Back to years' }).click()
  await expect(page.getByRole('button', { name: /2026/ })).toBeVisible()
  await expectNoHorizontalOverflow(page)
})
