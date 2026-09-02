import { useMemo, useState } from 'react'
import { type ProjectTypeView, useProjectTypes } from '../hooks/useProjectTypes'
import { DrilldownTimeChart } from './DrilldownTimeChart'
import { ProjectDetailView } from './ProjectDetailView'

export function ProjectTypeViews() {
  const projectTypes = useProjectTypes()
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)

  const selected = useMemo(() => {
    if (projectTypes.status !== 'ready') return null
    if (!selectedKey) return projectTypes.data[0] ?? null
    return projectTypes.data.find((item) => item.categoryKey === selectedKey) ?? null
  }, [projectTypes, selectedKey])

  if (selectedProjectId) return <ProjectDetailView repositoryId={selectedProjectId} onBack={() => setSelectedProjectId(null)} />
  if (projectTypes.status === 'loading') return <section className="dashboard-loading" aria-live="polite"><div className="loading-indicator" aria-hidden="true" /><p>Loading project types…</p></section>
  if (projectTypes.status === 'error') return <section className="dashboard-error" role="alert"><h2>Project type data could not be loaded.</h2><p>{projectTypes.error}</p></section>

  return <>
    <div className="view-toolbar"><div><p className="eyebrow">Project classification</p><h2>Project types</h2></div><span className="inventory-count">{projectTypes.data.length} categories</span></div>
    {projectTypes.data.length === 0 ? <section className="empty-inventory"><h3>No project categories yet.</h3><p>Run project classification before viewing project-type trends.</p></section> : (
      <div className="technology-layout">
        <section className="technology-list" aria-label="Project type list">
          {projectTypes.data.map((item) => <button type="button" key={item.categoryKey} className={`technology-list-item ${selected?.categoryKey === item.categoryKey ? 'technology-list-item-active' : ''}`} onClick={() => setSelectedKey(item.categoryKey)}><div><strong>{item.categoryName}</strong><span>{item.projectCount} projects</span></div><div className="technology-list-meta"><strong>{item.activityCount}</strong><span>commits</span></div></button>)}
        </section>
        {selected ? <ProjectTypeDetail key={selected.categoryKey} item={selected} onOpenProject={setSelectedProjectId} /> : null}
      </div>
    )}
  </>
}

function ProjectTypeDetail({ item, onOpenProject }: { item: ProjectTypeView; onOpenProject: (id:string)=>void }) {
  return <div className="technology-detail">
    <section className="project-detail-hero"><div><p className="eyebrow">Project category</p><h2>{item.categoryName}</h2><p>Evolution and activity for projects classified in this category. A repository may belong to more than one category.</p></div></section>
    <section className="metric-grid"><Metric label="Projects" value={item.projectCount} /><Metric label="Observed commits" value={item.activityCount} /><Metric label="Timeline months" value={item.timeline.length} /></section>
    <section className="dashboard-section"><span className="card-kicker">Evolution</span><h2>Category activity over time</h2><DrilldownTimeChart points={item.timeline.map(point => ({ month: point.month.slice(0, 7), commits: point.commits, changedLines: point.changedLines, lineStatisticsCommitCount: point.lineStatisticsCommitCount, secondary: `${point.activeProjectCount} active project${point.activeProjectCount === 1 ? '' : 's'}` }))} emptyText="No activity timeline available." /></section>
    <section className="dashboard-section"><span className="card-kicker">Matching work</span><h2>All matching projects</h2>{item.representativeProjects.length ? <div className="project-list">{item.representativeProjects.map((project) => <article className="project-row" key={project.repositoryId}><div><h3><button className="project-detail-link" type="button" onClick={() => onOpenProject(project.repositoryId)}>{project.repositoryName}</button></h3><p>{ownershipLabel(project.ownershipRelation)} · {project.visibility.toLowerCase()} · {project.contributionCount} contribution{project.contributionCount === 1 ? '' : 's'}</p></div><span className="representative-date">{formatDate(project.lastActivityAt)}</span></article>)}</div> : <p className="empty-state">No representative projects available.</p>}</section>
  </div>
}

function Metric({ label, value }: { label: string; value: string | number }) { return <article className="metric-card"><span>{label}</span><strong>{value}</strong></article> }
function ownershipLabel(value: string) { return value === 'OWNED_BY_USER' ? 'own' : 'external' }
function formatDate(value: string | null) { if (!value) return 'Unknown'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short' }).format(new Date(value)) }
