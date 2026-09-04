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
import { useProjectDetailNavigation } from '../hooks/useProjectDetailNavigation'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { countActiveAnalysisFilters } from '../analysis/AnalysisScope'

const navigationGroups = [
  { label: null, items: ['Overview'] },
  { label: 'Explore', items: ['Activity', 'Projects', 'Technologies', 'Project types', 'Contributions'] },
  { label: 'Insights', items: ['AI insights', 'Reports'] },
  { label: 'Settings', items: ['Privacy/data sources', 'Account'] },
] as const

type Section = (typeof navigationGroups)[number]['items'][number]

type Props = {
  user: SessionUser
}

export function AuthenticatedShell({ user }: Props) {
  const [section, setSection] = useState<Section>('Overview')
  const [mobileOpen, setMobileOpen] = useState(false)
  const { selectedProjectId, openProject, closeProject, clearProject } = useProjectDetailNavigation()
  const freshness = useDataFreshness(true)
  const { scope } = useAnalysisScope()
  const activeExploreFilterCount = countActiveAnalysisFilters(scope)

  const displayName = user.displayName.trim() || user.login

  function selectSection(next: Section) {
    clearProject()
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
            {navigationGroups.map((group, index) => (
              <div className="nav-group" key={group.label ?? 'overview'}>
                {group.label ? (
                  <div className="nav-group-label">
                    <span>{group.label}</span>
                    {group.label === 'Explore' && activeExploreFilterCount > 0 ? (
                      <span
                        className="nav-filter-count"
                        aria-label={`${activeExploreFilterCount} active ${activeExploreFilterCount === 1 ? 'filter' : 'filters'}`}
                      >
                        {activeExploreFilterCount}
                      </span>
                    ) : null}
                  </div>
                ) : null}
                <div className="nav-group-items">
                  {group.items.map((item) => (
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
                </div>
                {index === 0 ? <div className="nav-group-divider" aria-hidden="true" /> : null}
              </div>
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
            <ProjectDetailView repositoryId={selectedProjectId} onBack={closeProject} />
          ) : section === 'Overview' ? (
            <OverviewDashboard displayName={displayName} />
          ) : section === 'Activity' ? (
            <ActivityView onOpenProject={openProject} />
          ) : section === 'Projects' ? (
            <ProjectInventoryView />
          ) : section === 'Technologies' ? (
            <TechnologyViews />
          ) : section === 'Project types' ? (
            <ProjectTypeViews />
          ) : section === 'Contributions' ? (
            <ContributionsView />
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
