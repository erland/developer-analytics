import { useEffect, useState } from 'react'
import { getJson } from '../api/request'
import { appendChangeKinds, changeKindSelectionKey, type ChangeKind } from '../analysis/ChangeKind'

type Repository={id:string;visibility:string;ownershipRelation:string;includedInAnalysis:boolean;syncStatus:string}
type Activity={commitCount:number;activeProjects:number;additions:number;deletions:number;lineStatisticsCommitCount:number;firstActivityAt:string|null;lastActivityAt:string|null}
type TechnologyAssessment={technologyKey:string;technologyName:string;evidenceLevel:string;evidenceScore:number}
type ProjectType={categoryKey:string;categoryName:string;projectCount:number}
type SignificantProject={repositoryId:string;repositoryName:string;matchReason:string;significanceScore:number;involvementScore:number}
export type OverviewDashboardData={repositoriesAnalysed:number;ownRepositories:number;externalRepositories:number;publicRepositories:number;privateRepositories:number;commits:number;linesChanged:number;netLinesContributed:number;lineStatisticsCommitCount:number;firstActivityAt:string|null;lastActivityAt:string|null;activeProjects:number;keyTechnologies:TechnologyAssessment[];projectCategories:Array<{categoryKey:string;categoryName:string;confidence:string}>;significantProjects:SignificantProject[]}
type State={status:'loading';data:null;error:null}|{status:'ready';data:OverviewDashboardData;error:null}|{status:'error';data:null;error:string}

const REFRESH_INTERVAL_MS = 10_000

export function useOverviewDashboard(enabled:boolean,changeKinds:readonly ChangeKind[]):State{
 const [state,setState]=useState<State>({status:'loading',data:null,error:null});const key=changeKindSelectionKey(changeKinds)
 useEffect(()=>{
  if(!enabled)return
  let cancelled=false
  let controller:AbortController|null=null
  async function load(){
   controller?.abort()
   const current=new AbortController()
   controller=current
   try{
    const activityParams=new URLSearchParams();appendChangeKinds(activityParams,changeKinds);const suffix=activityParams.size?`?${activityParams}`:''
    const [repositories,activity,technologies,projectTypes,significantProjects]=await Promise.all([
     getJson<Repository[]>('/api/me/repositories',{signal:current.signal,errorMessage:'/api/me/repositories failed'}),
     getJson<Activity>(`/api/me/activity${suffix}`,{signal:current.signal,errorMessage:'/api/me/activity failed'}),
     getJson<TechnologyAssessment[]>(`/api/me/technologies${suffix}`,{signal:current.signal,errorMessage:'/api/me/technologies failed'}),
     getJson<ProjectType[]>(`/api/me/project-types${suffix}`,{signal:current.signal,errorMessage:'/api/me/project-types failed'}),
     getJson<SignificantProject[]>('/api/me/significant-external-projects',{signal:current.signal,errorMessage:'/api/me/significant-external-projects failed'})
    ])
    if(cancelled||current.signal.aborted)return
    const included=repositories.filter(r=>r.includedInAnalysis!==false)
    setState({status:'ready',error:null,data:{repositoriesAnalysed:included.length,ownRepositories:included.filter(r=>r.ownershipRelation==='OWNED_BY_USER').length,externalRepositories:included.filter(r=>r.ownershipRelation!=='OWNED_BY_USER').length,publicRepositories:included.filter(r=>r.visibility==='PUBLIC').length,privateRepositories:included.filter(r=>r.visibility==='PRIVATE').length,commits:activity.commitCount,linesChanged:(activity.additions??0)+(activity.deletions??0),netLinesContributed:(activity.additions??0)-(activity.deletions??0),lineStatisticsCommitCount:activity.lineStatisticsCommitCount??0,firstActivityAt:activity.firstActivityAt,lastActivityAt:activity.lastActivityAt,activeProjects:activity.activeProjects,keyTechnologies:technologies.slice(0,8),projectCategories:projectTypes.slice(0,8).map(x=>({categoryKey:x.categoryKey,categoryName:x.categoryName,confidence:`${x.projectCount} projects`})),significantProjects:significantProjects.slice(0,8)}})
   }catch(e){
    if(!cancelled&&!current.signal.aborted)setState(previous=>previous.status==='ready'?previous:{status:'error',data:null,error:e instanceof Error?e.message:'Unable to load overview'})
   }
  }
  void load()
  const timer=window.setInterval(()=>void load(),REFRESH_INTERVAL_MS)
  return()=>{cancelled=true;controller?.abort();window.clearInterval(timer)}
 },[enabled,key])
 return state
}
