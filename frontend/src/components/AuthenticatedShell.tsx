import { useState } from 'react'
import type { SessionUser } from '../hooks/useAuthenticatedSession'
import { useDataFreshness } from '../hooks/useDataFreshness'
import { OverviewDashboard } from './OverviewDashboard'
import { ActivityView } from './ActivityView'
import { ProjectInventoryView } from './ProjectInventoryView'
import { TechnologyViews } from './TechnologyViews'
import { ProjectTypeViews } from './ProjectTypeViews'
import { PrivacyDataSourcesView } from './PrivacyDataSourcesView'
import { ReportsView } from './ReportsView'
import { AiInsightsView } from './AiInsightsView'
import { AccountView } from './AccountView'
import { ContributionsView } from './ContributionsView'
import { ProjectDetailView } from './ProjectDetailView'

const sections = [
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
] as const

type Section = (typeof sections)[number]

type Props = {
  user: SessionUser
}

export function AuthenticatedShell({ user }: Props) {
  const [section, setSection] = useState<Section>('Overview')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const freshness = useDataFreshness(true)

  const displayName = user.displayName.trim() || user.login

  function selectSection(next: Section) {
    setSelectedProjectId(null)
    setSection(next)
    setMobileOpen(false)
  }

  return (
    <div className="authenticated-shell">
      <header className="app-header">
        <button
          className="mobile-menu-button"
          type="button"
          aria-expanded={mobileOpen}
          aria-controls="primary-navigation"
          onClick={() => setMobileOpen((value) => !value)}
        >
          <span aria-hidden="true">☰</span>
          <span className="sr-only">Toggle navigation</span>
        </button>

        <a className="brand" href="/" aria-label="Developer Analytics home">
          Developer Analytics
        </a>

        <div className="header-user">
          <div className={`freshness freshness-${freshness.status}`} title={freshness.timestamp ?? undefined}>
            <span className="freshness-dot" aria-hidden="true" />
            <span>{freshness.label}</span>
          </div>
          <span className="user-name">{displayName}</span>
        </div>
      </header>

      <div className="workspace">
        <aside
          id="primary-navigation"
          className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}
          aria-label="Primary"
        >
          <nav>
            {sections.map((item) => (
              <button
                key={item}
                type="button"
                className={`nav-item ${section === item ? 'nav-item-active' : ''}`}
                aria-current={section === item ? 'page' : undefined}
                onClick={() => selectSection(item)}
              >
                {item}
              </button>
            ))}
          </nav>

          <div className="sidebar-footer">
            <span className="provider-label">Connected through {user.provider}</span>
            <form method="post" action="/api/auth/logout">
              <button className="text-button" type="submit">Sign out</button>
            </form>
          </div>
        </aside>

        {mobileOpen ? (
          <button
            className="navigation-scrim"
            aria-label="Close navigation"
            type="button"
            onClick={() => setMobileOpen(false)}
          />
        ) : null}

        <main className="workspace-content">
          <div className="page-heading">
            <div>
              <p className="eyebrow">Private developer analytics</p>
              <h1>{section}</h1>
            </div>
            <div className={`freshness freshness-page freshness-${freshness.status}`}>
              <span className="freshness-dot" aria-hidden="true" />
              <span>{freshness.label}</span>
            </div>
          </div>

          {selectedProjectId ? (
            <ProjectDetailView repositoryId={selectedProjectId} onBack={() => setSelectedProjectId(null)} />
          ) : section === 'Overview' ? (
            <OverviewDashboard displayName={displayName} />
          ) : section === 'Activity' ? (
            <ActivityView />
          ) : section === 'Projects' ? (
            <ProjectInventoryView />
          ) : section === 'Technologies' ? (
            <TechnologyViews />
          ) : section === 'Project types' ? (
            <ProjectTypeViews />
          ) : section === 'Contributions' ? (
            <ContributionsView onOpenProject={setSelectedProjectId} />
          ) : section === 'AI insights' ? (
            <AiInsightsView />
          ) : section === 'Reports' ? (
            <ReportsView />
          ) : section === 'Privacy/data sources' ? (
            <PrivacyDataSourcesView />
          ) : section === 'Account' ? (
            <AccountView />
          ) : (
            <section className="placeholder-panel" aria-labelledby="section-heading">
              <h2 id="section-heading">{section}</h2>
              <p>
                This section is connected to the authenticated application shell and is ready
                for its dashboard content in the next implementation steps.
              </p>
            </section>
          )}
        </main>
      </div>
    </div>
  )
}
