import { useMemo, useState } from 'react'
import { type TechnologyView, useTechnologyViews } from '../hooks/useTechnologyViews'
import { setTechnologySuppressed } from '../hooks/useCorrections'
import { DrilldownTimeChart } from './DrilldownTimeChart'
import { ProjectDetailView } from './ProjectDetailView'

export function TechnologyViews() {
  const technologies = useTechnologyViews()
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const selected = useMemo(() => {
    if (technologies.status !== 'ready') return null
    if (!selectedKey) return technologies.data[0] ?? null
    return technologies.data.find((item) => item.technologyKey === selectedKey) ?? null
  }, [selectedKey, technologies])
  if (selectedProjectId) return <ProjectDetailView repositoryId={selectedProjectId} onBack={() => setSelectedProjectId(null)} />
  if (technologies.status === 'loading') return <section className="dashboard-loading" aria-live="polite"><div className="loading-indicator" aria-hidden="true" /><p>Loading technologies…</p></section>
  if (technologies.status === 'error') return <section className="dashboard-error" role="alert"><h2>Technology data could not be loaded.</h2><p>{technologies.error}</p></section>
  return <>
    <div className="view-toolbar"><div><p className="eyebrow">Evidence-based technology history</p><h2>Technologies</h2></div><span className="inventory-count">{technologies.data.length} technologies</span></div>
    {technologies.data.length === 0 ? <section className="empty-inventory"><h3>No technology assessments yet.</h3><p>Collect repository evidence and recalculate technology assessments first.</p></section> : <div className="technology-layout">
      <section className="technology-list" aria-label="Technology list">{technologies.data.map((technology) => <button type="button" key={technology.technologyKey} className={`technology-list-item ${selected?.technologyKey === technology.technologyKey ? 'technology-list-item-active' : ''}`} onClick={() => setSelectedKey(technology.technologyKey)}><div><strong>{technology.technologyName}</strong><span>{humanize(technology.technologyCategory)}</span></div><div className="technology-list-meta"><strong>{technology.evidenceLevel}</strong><span>{technology.projectCount} projects</span></div></button>)}</section>
      {selected ? <TechnologyDetail key={selected.technologyKey} technology={selected} onOpenProject={setSelectedProjectId} /> : null}
    </div>}
  </>
}

function TechnologyDetail({ technology, onOpenProject }: { technology: TechnologyView; onOpenProject:(id:string)=>void }) {
  return <div className="technology-detail">
    <section className="project-detail-hero"><div><p className="eyebrow">Technology evidence</p><h2>{technology.technologyName}</h2><p>Evidence level {technology.evidenceLevel.toLowerCase()} based on observed repository signals. This is not a formal proficiency rating.</p><span className="privacy-provenance">{privacyLabel(technology.privacyProvenance)}</span></div><div className="technology-score"><strong>{technology.evidenceScore}</strong><span>evidence score</span></div></section>
    <section className="dashboard-section correction-panel"><span className="card-kicker">Correction</span><h2>Technology inference</h2><p className="settings-intro">Suppressing this inference hides it from analysis views and AI profile conclusions while retaining the underlying repository evidence.</p><button className="secondary-action" type="button" onClick={async () => { await setTechnologySuppressed(technology.technologyKey, true); window.location.reload() }}>Suppress technology inference</button></section>
    <section className="metric-grid"><Metric label="Projects" value={technology.projectCount} /><Metric label="Evidence items" value={technology.evidenceCount} /><Metric label="Evidence types" value={technology.independentEvidenceTypes} /><Metric label="Recent projects" value={technology.recentProjectCount} /><Metric label="First observed" value={formatDate(technology.firstObservedAt)} compact /><Metric label="Latest observed" value={formatDate(technology.lastObservedAt)} compact /></section>
    <section className="dashboard-section"><span className="card-kicker">Timeline</span><h2>Activity over time</h2><DrilldownTimeChart points={technology.timeline.map(point => ({ month: point.month.slice(0, 7), commits: point.commits, changedLines: point.changedLines, lineStatisticsCommitCount: point.lineStatisticsCommitCount, secondary: `${point.projectCount} project${point.projectCount === 1 ? '' : 's'}` }))} emptyText="No timeline aggregate yet." /></section>
    <section className="dashboard-section"><span className="card-kicker">Matching work</span><h2>All matching projects</h2>{technology.representativeProjects.length ? <div className="project-list">{technology.representativeProjects.map((project) => <article className="project-row" key={project.repositoryId}><div><h3><button className="project-detail-link" type="button" onClick={() => onOpenProject(project.repositoryId)}>{project.repositoryName}</button></h3><p>{ownershipLabel(project.ownershipRelation)} · {project.visibility.toLowerCase()} · {project.evidenceCount} evidence item{project.evidenceCount === 1 ? '' : 's'}</p></div><span className="representative-date">{formatDate(project.lastActivityAt)}</span></article>)}</div> : <p className="empty-state">No matching projects available.</p>}</section>
  </div>
}

function Metric({ label, value, compact = false }: { label: string; value: string | number; compact?: boolean }) { return <article className="metric-card"><span>{label}</span><strong className={compact ? 'metric-value-compact' : undefined}>{value}</strong></article> }
function ownershipLabel(value: string) { return value === 'OWNED_BY_USER' ? 'own' : 'external' }
function humanize(value: string) { return value.toLowerCase().replaceAll('_', ' ') }
function formatDate(value: string | null) { if (!value) return 'Unknown'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short' }).format(new Date(value)) }
function privacyLabel(value: TechnologyView['privacyProvenance']) { if (value === 'PRIVATE_AGGREGATE') return 'Private aggregate'; if (value === 'INCLUDES_PRIVATE') return 'Includes private data'; return 'Public data only' }
