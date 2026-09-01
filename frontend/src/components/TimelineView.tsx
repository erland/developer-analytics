import { useMemo, useState } from 'react'
import { useActivityView } from '../hooks/useActivityView'

export function TimelineView() {
  const activity = useActivityView('all')
  const [query, setQuery] = useState('')

  if (activity.status === 'loading') {
    return <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading timeline…</p></section>
  }
  if (activity.status === 'error') {
    return <section className="dashboard-error"><h2>Timeline could not be loaded.</h2><p>{activity.error}</p></section>
  }

  const projects = activity.data.projectsOverTime
  const q = query.trim().toLowerCase()
  const filtered = projects
    .filter(project => !q || project.repositoryName.toLowerCase().includes(q))
    .sort((a,b) => new Date(a.firstActivityAt).getTime() - new Date(b.firstActivityAt).getTime())

  return <>
    <div className="view-toolbar">
      <div><p className="eyebrow">Project history</p><h2>Projects over time</h2></div>
      <label className="period-filter"><span>Filter projects</span><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Project name" /></label>
    </div>
    {projects.length ? <CompactTimeline projects={filtered} /> : <section className="dashboard-section"><p className="empty-state">No project activity has been collected yet.</p></section>}
  </>
}

function CompactTimeline({ projects }: { projects: Array<{ repositoryId:string; repositoryName:string; firstActivityAt:string; lastActivityAt:string; commits:number; monthlyActivity:Array<{month:string;commits:number}> }> }) {
  const months = useMemo(() => {
    if (!projects.length) return []
    const first = new Date(Math.min(...projects.map(project => new Date(project.firstActivityAt).getTime())))
    const last = new Date(Math.max(...projects.map(project => new Date(project.lastActivityAt).getTime())))
    const result:string[] = []
    let year = first.getUTCFullYear()
    let month = first.getUTCMonth()
    while (year < last.getUTCFullYear() || (year === last.getUTCFullYear() && month <= last.getUTCMonth())) {
      result.push(`${year}-${String(month + 1).padStart(2,'0')}`)
      month++
      if (month === 12) { month = 0; year++ }
    }
    return result
  }, [projects])

  if (!projects.length) return <section className="dashboard-section"><p className="empty-state">No projects match the filter.</p></section>

  const monthWidth = 12
  const nameWidth = 190
  const commitsWidth = 64
  const stripWidth = Math.max(1, months.length) * monthWidth
  const gridStyle = { display:'grid', gridTemplateColumns:`${nameWidth}px ${stripWidth}px ${commitsWidth}px`, alignItems:'center' } as const
  const stripStyle = { display:'grid', gridTemplateColumns:`repeat(${Math.max(1, months.length)}, ${monthWidth}px)`, minWidth:`${stripWidth}px` } as const

  return <section className="dashboard-section">
    <p className="settings-intro">One narrow cell represents one month. Stronger cells mean more commits in that project. Scroll horizontally through time and vertically through projects.</p>
    <div style={{overflow:'auto', maxHeight:'68vh', border:'1px solid #e2e8f0', borderRadius:'.75rem'}}>
      <div style={{minWidth:`${nameWidth + stripWidth + commitsWidth}px`}}>
        <div style={{...gridStyle, position:'sticky', top:0, zIndex:2, background:'#fff', borderBottom:'1px solid #e2e8f0', minHeight:'34px', fontSize:'.72rem', color:'#64748b'}}>
          <strong style={{position:'sticky', left:0, zIndex:3, background:'#fff', padding:'0 .65rem'}}>Project</strong>
          <div style={stripStyle}>{months.map(month => <span key={month} title={month} style={{fontSize:'.62rem', overflow:'visible'}}>{month.endsWith('-01') ? month.slice(0,4) : ''}</span>)}</div>
          <strong style={{textAlign:'right', paddingRight:'.65rem'}}>Commits</strong>
        </div>
        {projects.map(project => {
          const byMonth = new Map(project.monthlyActivity.map(value => [value.month, value.commits]))
          const max = Math.max(1, ...project.monthlyActivity.map(value => value.commits))
          return <div key={project.repositoryId} style={{...gridStyle, minHeight:'28px', borderBottom:'1px solid #f1f5f9', fontSize:'.75rem'}}>
            <span title={project.repositoryName} style={{position:'sticky', left:0, zIndex:1, background:'#fff', padding:'0 .65rem', whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'}}>{project.repositoryName}</span>
            <div style={stripStyle}>{months.map(month => {
              const value = byMonth.get(month) ?? 0
              return <span key={month} title={`${project.repositoryName} · ${month}: ${value} commits`} style={{height:'16px', alignSelf:'center', margin:'0 1px', borderRadius:'2px', background:value ? '#334155' : '#f1f5f9', opacity:value ? 0.18 + 0.82 * (value / max) : 1}} />
            })}</div>
            <span style={{textAlign:'right', paddingRight:'.65rem'}}>{new Intl.NumberFormat().format(project.commits)}</span>
          </div>
        })}
      </div>
    </div>
  </section>
}
