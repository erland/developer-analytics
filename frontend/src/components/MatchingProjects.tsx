import { useEffect, useState } from 'react'
import type { AnalysisScope } from '../analysis/AnalysisScope'
import { useMatchingProjects } from '../hooks/useMatchingProjects'
import { useProjectDetailNavigation } from '../hooks/useProjectDetailNavigation'
import { ProjectDetailView } from './ProjectDetailView'
import { AnalysisEmptyState } from './AnalysisEmptyState'

export function MatchingProjects({
  scope,
  title = 'Projects matching this selection',
  pageSize = 25,
  onScopeChange,
}: {
  scope: AnalysisScope
  title?: string
  pageSize?: number
  onScopeChange?: (scope: AnalysisScope) => void
}) {
  const [page, setPage] = useState(0)
  const { selectedProjectId, openProject, closeProject } = useProjectDetailNavigation()
  const projects = useMatchingProjects(scope, page, pageSize)

  useEffect(() => {
    setPage(0)
  }, [scope])

  if (selectedProjectId) {
    return (
      <ProjectDetailView
        repositoryId={selectedProjectId}
        onBack={closeProject}
      />
    )
  }

  return (
    <section className="dashboard-section matching-projects" aria-label={title}>
      <div className="matching-projects-heading">
        <div>
          <span className="card-kicker">Matching work</span>
          <h2>{title}</h2>
        </div>
        {projects.status === 'ready' ? (
          <span className="inventory-count">{projects.data.total} projects</span>
        ) : null}
      </div>

      {projects.status === 'loading' ? (
        <div className="dashboard-loading" aria-live="polite">
          <div className="loading-indicator" aria-hidden="true" />
          <p>Loading matching projects…</p>
        </div>
      ) : null}

      {projects.status === 'error' ? (
        <div className="dashboard-error" role="alert">
          <h3>Matching projects could not be loaded.</h3>
          <p>{projects.error}</p>
        </div>
      ) : null}

      {projects.status === 'ready' ? (
        <>
          {projects.data.items.length ? (
            <div className="project-list">
              {projects.data.items.map((project) => (
                <article className="project-row" key={project.id}>
                  <div>
                    <h3>
                      <button
                        className="project-detail-link"
                        type="button"
                        onClick={() => openProject(project.id)}
                      >
                        {project.name}
                      </button>
                    </h3>
                    <p>
                      {ownershipLabel(project.ownershipRelation)} · {project.visibility.toLowerCase()}
                      {project.categories.length ? ` · ${project.categories.map((item) => item.name).join(', ')}` : ''}
                    </p>
                    {project.technologies.length ? (
                      <p className="matching-project-technologies">
                        {project.technologies.map((item) => item.name).join(' · ')}
                      </p>
                    ) : null}
                  </div>
                  <span className="representative-date">{formatDate(project.lastActivityAt)}</span>
                </article>
              ))}
            </div>
          ) : (
            <AnalysisEmptyState
              className="empty-inventory"
              title="No projects match the current selection."
              description="Broaden the analysis selection to see projects again."
              scope={scope}
              onScopeChange={onScopeChange}
            />
          )}

          <MatchingProjectsPagination
            page={projects.data.page}
            totalPages={projects.data.totalPages}
            onChange={setPage}
          />
        </>
      ) : null}
    </section>
  )
}

function MatchingProjectsPagination({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  return (
    <nav className="pagination" aria-label="Matching project pages">
      <button type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span>Page {page + 1} of {totalPages}</span>
      <button type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </nav>
  )
}

function ownershipLabel(value: string) {
  return value === 'OWNED_BY_USER' ? 'own' : 'external'
}

function formatDate(value: string | null) {
  if (!value) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}
