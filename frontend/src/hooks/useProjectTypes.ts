import { useEffect, useState } from 'react'
import { getJson } from '../api/request'
import { ALL_CHANGE_KINDS, appendChangeKinds, changeKindSelectionKey, type ChangeKind } from '../analysis/ChangeKind'

export type ProjectTypeView = {
  categoryKey:string; categoryName:string; projectCount:number; activityCount:number
  timeline:Array<{month:string;commits:number;changedLines:number;lineStatisticsCommitCount:number;activeProjectCount:number}>
  representativeProjects:Array<{repositoryId:string;repositoryName:string;htmlUrl:string|null;visibility:string;ownershipRelation:string;lastActivityAt:string|null;contributionCount:number}>
}
type State={status:'loading';data:null;error:null}|{status:'ready';data:ProjectTypeView[];error:null}|{status:'error';data:null;error:string}

export function useProjectTypes(changeKinds: readonly ChangeKind[] = ALL_CHANGE_KINDS):State{
 const [state,setState]=useState<State>({status:'loading',data:null,error:null}); const key=changeKindSelectionKey(changeKinds)
 useEffect(()=>{const controller=new AbortController(); async function load(){try{const params=new URLSearchParams();appendChangeKinds(params,changeKinds);const data=await getJson<ProjectTypeView[]>(`/api/me/project-types${params.size?`?${params}`:''}`,{signal:controller.signal,errorMessage:'Project type request failed'});setState({status:'ready',data,error:null})}catch(error){if(controller.signal.aborted)return;setState({status:'error',data:null,error:error instanceof Error?error.message:'Unable to load project types'})}}void load();return()=>controller.abort()},[key,changeKinds]);return state
}
