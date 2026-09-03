import { useEffect, useState } from 'react'
import { type AnalysisScope } from '../analysis/AnalysisScope'
import { projectFacetOptions } from '../analysis/ProjectFacetOptions'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { useProjectInventory } from '../hooks/useProjectInventory'
import { useProjectDetailNavigation } from '../hooks/useProjectDetailNavigation'
import { AnalysisFilters } from './AnalysisFilters'
import { AnalysisEmptyState } from './AnalysisEmptyState'
import { ProjectDetailView } from './ProjectDetailView'

export function ProjectInventoryView() {
  const { scope, pushScope, replaceScope } = useAnalysisScope()
  const [page, setPage] = useState(0)
  const [activity, setActivity] = useState('')
  const { selectedProjectId, openProject, closeProject } = useProjectDetailNavigation()
  const inventory = useProjectInventory({ page, pageSize: 25, activity, scope })

  useEffect(() => {
    setPage(0)
  }, [scope])

  function updateScope(nextScope: AnalysisScope, historyMode: 'push' | 'replace' = 'push') {
    setPage(0)
    if (historyMode === 'replace') {
      replaceScope(nextScope)
    } else {
      pushScope(nextScope)
    }
  }

  function updateSearch(value: string) {
    updateScope({ ...scope, search: value || undefined }, 'replace')
  }

  function updateVisibility(value: string) {
    updateScope({
      ...scope,
      visibility: value === 'public' || value === 'private' ? value : undefined,
    })
  }

  const technologyOptions = inventory.status === 'ready'
    ? projectFacetOptions(inventory.data.facets.technologies)
    : []

  const projectTypeOptions = inventory.status === 'ready'
    ? projectFacetOptions(inventory.data.facets.projectTypes)
    : []

  if (selectedProjectId) {
    return (
      <ProjectDetailView
        repositoryId={selectedProjectId}
        onBack={closeProject}
      />
    )
  }

  return (
    <>
      <div className="view-toolbar project-toolbar">
        <div>
          <p className="eyebrow">Repository inventory</p>
          <h2>Projects</h2>
        </div>
        {inventory.status === 'ready' ? (
          <span className="inventory-count">{inventory.data.total} projects</span>
        ) : null}
      </div>

      <AnalysisFilters
        scope={scope}
        onChange={updateScope}
        technologies={technologyOptions}
        projectTypes={projectTypeOptions}
        showTechnology
        showProjectType
        showOwnership
      />

      <section className="project-filter-group" aria-labelledby="project-filter-heading">
        <div className="project-filter-group-heading">
          <div>
            <h3 id="project-filter-heading">Project filters</h3>
            <p>These filters are part of the analysis selection and are kept in the URL.</p>
          </div>
        </div>
        <div className="inventory-filters project-scope-filters">
          <label>
            <span>Search</span>
            <input
              type="text"
              inputMode="search"
              enterKeyHint="search"
              aria-label="Search projects"
              value={scope.search ?? ''}
              placeholder="Project name or description"
              onChange={(event) => updateSearch(event.target.value)}
            />
          </label>

          <FilterSelect
            label="Visibility"
            value={scope.visibility ?? ''}
            onChange={updateVisibility}
            options={[
              ['', 'All'],
              ['public', 'Public'],
              ['private', 'Private'],
            ]}
          />
        </div>
      </section>

      <section className="project-filter-group project-list-options" aria-labelledby="project-list-options-heading">
        <div className="project-filter-group-heading">
          <div>
            <h3 id="project-list-options-heading">Project list options</h3>
            <p>These options only refine how the project inventory is shown and are not part of the analysis scope.</p>
          </div>
        </div>
        <div className="inventory-filters project-list-option-fields">
          <FilterSelect
            label="Activity"
            value={activity}
            onChange={(value) => {
              setActivity(value)
              setPage(0)
            }}
            options={[
              ['', 'All'],
              ['active', 'Active'],
              ['inactive', 'Inactive'],
            ]}
          />
        </div>
      </section>

      {inventory.status === 'loading' ? (
        <section className="dashboard-loading" aria-live="polite">
          <div className="loading-indicator" aria-hidden="true" />
          <p>Loading projects…</p>
        </section>
      ) : null}

      {inventory.status === 'error' ? (
        <section className="dashboard-error" role="alert">
          <h2>Project inventory could not be loaded.</h2>
          <p>{inventory.error}</p>
        </section>
      ) : null}

      {inventory.status === 'ready' ? (
        <>
          <div className="project-inventory-grid">
            {inventory.data.items.length ? (
              inventory.data.items.map((project) => (
                <article className="inventory-card" key={project.id}>
                  <div className="inventory-card-heading">
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
                      <p>{project.description || 'No repository description.'}</p>
                    </div>
                    <div className="inventory-badges">
                      <span>{ownershipLabel(project.ownershipRelation)}</span>
                      <span>{project.visibility.toLowerCase()}</span>
                      <span>{activityLabel(project.lastActivityAt)}</span>
                    </div>
                  </div>

                  <InventoryTags
                    label="Categories"
                    values={project.categories.map((item) => item.name)}
                  />
                  <InventoryTags
                    label="Technologies"
                    values={project.technologies.map((item) => item.name)}
                  />

                  <div className="inventory-meta">
                    Last activity: {formatDate(project.lastActivityAt)}
                  </div>
                </article>
              ))
            ) : (
              <AnalysisEmptyState
                className="empty-inventory"
                title="No projects match the current selection."
                description="Broaden the analysis selection or project-list options to see projects again."
                scope={scope}
                onScopeChange={updateScope}
                extraAction={activity ? {
                  label: 'Clear activity option',
                  onClick: () => { setActivity(''); setPage(0) },
                } : undefined}
              />
            )}
          </div>

          <Pagination
            page={inventory.data.page}
            totalPages={inventory.data.totalPages}
            onChange={setPage}
          />
        </>
      ) : null}
    </>
  )
}

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string
  value: string
  options: string[][]
  onChange: (value: string) => void
}) {
  return (
    <label>
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map(([optionValue, optionLabel]) => (
          <option value={optionValue} key={optionValue || 'all'}>
            {optionLabel}
          </option>
        ))}
      </select>
    </label>
  )
}

function InventoryTags({
  label,
  values,
}: {
  label: string
  values: string[]
}) {
  if (!values.length) return null

  return (
    <div className="inventory-tag-row">
      <span>{label}</span>
      <div>
        {values.slice(0, 6).map((value) => (
          <span className="inventory-tag" key={value}>{value}</span>
        ))}
      </div>
    </div>
  )
}

function Pagination({
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
    <nav className="pagination" aria-label="Project pages">
      <button
        type="button"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        Previous
      </button>
      <span>Page {page + 1} of {totalPages}</span>
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  )
}

function ownershipLabel(value: string) {
  return value === 'OWNED_BY_USER' ? 'own' : 'external'
}

function activityLabel(lastActivityAt: string | null) {
  if (!lastActivityAt) return 'inactive'
  const date = new Date(lastActivityAt)
  return Date.now() - date.getTime() <= 365 * 24 * 60 * 60 * 1000
    ? 'active'
    : 'inactive'
}

function formatDate(value: string | null) {
  if (!value) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}
