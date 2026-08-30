import { useMemo, useState } from 'react'
import {
  initialInventoryFilters,
  type InventoryFilters,
  useProjectInventory,
} from '../hooks/useProjectInventory'
import { ProjectDetailView } from './ProjectDetailView'

export function ProjectInventoryView() {
  const [filters, setFilters] = useState<InventoryFilters>(initialInventoryFilters)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const inventory = useProjectInventory(filters)

  function update<K extends keyof InventoryFilters>(
    key: K,
    value: InventoryFilters[K],
  ) {
    setFilters((current) => ({
      ...current,
      [key]: value,
      page: key === 'page' ? (value as number) : 0,
    }))
  }

  const knownCategories = useMemo(() => {
    if (inventory.status !== 'ready') return []
    const map = new Map<string, string>()
    for (const project of inventory.data.items) {
      for (const category of project.categories) map.set(category.key, category.name)
    }
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [inventory])

  const knownTechnologies = useMemo(() => {
    if (inventory.status !== 'ready') return []
    const map = new Map<string, string>()
    for (const project of inventory.data.items) {
      for (const technology of project.technologies) map.set(technology.key, technology.name)
    }
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [inventory])

  if (selectedProjectId) {
    return (
      <ProjectDetailView
        repositoryId={selectedProjectId}
        onBack={() => setSelectedProjectId(null)}
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

      <section className="inventory-filters" aria-label="Project filters">
        <label>
          <span>Search</span>
          <input
            type="text"
            inputMode="search"
            enterKeyHint="search"
            aria-label="Search projects"
            value={filters.search}
            placeholder="Project name or description"
            onChange={(event) => update('search', event.target.value)}
          />
        </label>

        <FilterSelect
          label="Ownership"
          value={filters.ownership}
          onChange={(value) => update('ownership', value)}
          options={[
            ['', 'All'],
            ['own', 'Own'],
            ['external', 'External'],
          ]}
        />

        <FilterSelect
          label="Visibility"
          value={filters.visibility}
          onChange={(value) => update('visibility', value)}
          options={[
            ['', 'All'],
            ['public', 'Public'],
            ['private', 'Private'],
          ]}
        />

        <FilterSelect
          label="Activity"
          value={filters.activity}
          onChange={(value) => update('activity', value)}
          options={[
            ['', 'All'],
            ['active', 'Active'],
            ['inactive', 'Inactive'],
          ]}
        />

        <FilterSelect
          label="Category"
          value={filters.category}
          onChange={(value) => update('category', value)}
          options={[
            ['', 'All'],
            ...knownCategories,
          ]}
        />

        <FilterSelect
          label="Technology"
          value={filters.technology}
          onChange={(value) => update('technology', value)}
          options={[
            ['', 'All'],
            ...knownTechnologies,
          ]}
        />
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
                          onClick={() => setSelectedProjectId(project.id)}
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
              <section className="empty-inventory">
                <h3>No projects match these filters.</h3>
                <p>Change one or more filters to broaden the inventory.</p>
              </section>
            )}
          </div>

          <Pagination
            page={inventory.data.page}
            totalPages={inventory.data.totalPages}
            onChange={(page) => update('page', page)}
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
