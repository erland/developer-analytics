import { useEffect, useState } from 'react'

type Repository = { id: string; visibility: string; ownershipRelation: string; includedInAnalysis: boolean; syncStatus: string }
type Activity = { commitCount: number; activeProjects: number; firstActivityAt: string | null; lastActivityAt: string | null }
type TechnologyAssessment = { technologyKey: string; technologyName: string; evidenceLevel: string; evidenceScore: number }
type ProjectType = { categoryKey: string; categoryName: string; projectCount: number }
type SignificantProject = { repositoryId: string; repositoryName: string; matchReason: string; significanceScore: number; involvementScore: number }

export type OverviewDashboardData = {
  repositoriesAnalysed: number; ownRepositories: number; externalRepositories: number; publicRepositories: number; privateRepositories: number;
  commits: number; firstActivityAt: string | null; lastActivityAt: string | null; activeProjects: number;
  keyTechnologies: TechnologyAssessment[]; projectCategories: Array<{categoryKey:string;categoryName:string;confidence:string}>; significantProjects: SignificantProject[]
}
type State = { status:'loading'; data:null; error:null } | { status:'ready'; data:OverviewDashboardData; error:null } | {status:'error';data:null;error:string}
async function getJson<T>(url:string, signal:AbortSignal):Promise<T>{ const r=await fetch(url,{credentials:'include',headers:{Accept:'application/json'},signal}); if(!r.ok) throw new Error(`${url} failed with HTTP ${r.status}`); return await r.json() as T }

export function useOverviewDashboard(enabled:boolean):State {
 const [state,setState]=useState<State>({status:'loading',data:null,error:null})
 useEffect(()=>{ if(!enabled)return; const c=new AbortController();
  async function load(){ try {
   const [repositories,activity,technologies,projectTypes,significantProjects]=await Promise.all([
    getJson<Repository[]>('/api/me/repositories',c.signal), getJson<Activity>('/api/me/activity',c.signal),
    getJson<TechnologyAssessment[]>('/api/me/technologies',c.signal), getJson<ProjectType[]>('/api/me/project-types',c.signal),
    getJson<SignificantProject[]>('/api/me/significant-external-projects',c.signal)])
   const included=repositories.filter(r=>r.includedInAnalysis !== false)
   setState({status:'ready',error:null,data:{
    repositoriesAnalysed:included.length,
    ownRepositories:included.filter(r=>r.ownershipRelation==='OWNED_BY_USER').length,
    externalRepositories:included.filter(r=>r.ownershipRelation!=='OWNED_BY_USER').length,
    publicRepositories:included.filter(r=>r.visibility==='PUBLIC').length,
    privateRepositories:included.filter(r=>r.visibility==='PRIVATE').length,
    commits:activity.commitCount, firstActivityAt:activity.firstActivityAt,lastActivityAt:activity.lastActivityAt,activeProjects:activity.activeProjects,
    keyTechnologies:technologies.slice(0,8).map(t=>({technologyKey:t.technologyKey,technologyName:t.technologyName,strength:t.evidenceLevel,score:t.evidenceScore})), projectCategories:projectTypes.slice(0,8).map(x=>({categoryKey:x.categoryKey,categoryName:x.categoryName,confidence:`${x.projectCount} projects`})), significantProjects:significantProjects.slice(0,8)
   }})
  } catch(e){ if(!c.signal.aborted)setState({status:'error',data:null,error:e instanceof Error?e.message:'Unable to load overview'}) }}
  void load(); return()=>c.abort()
 },[enabled]); return state
}
