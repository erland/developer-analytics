export type ChangeKind = 'CODE' | 'DOCUMENTATION' | 'CI_CD' | 'OTHER'

export const ALL_CHANGE_KINDS: readonly ChangeKind[] = ['CODE', 'DOCUMENTATION', 'CI_CD', 'OTHER']

export function appendChangeKinds(params: URLSearchParams, changeKinds: readonly ChangeKind[]) {
  if (isAllChangeKinds(changeKinds)) return params
  params.set('changeKinds', changeKinds.join(','))
  return params
}

export function isAllChangeKinds(changeKinds: readonly ChangeKind[]) {
  return changeKinds.length === ALL_CHANGE_KINDS.length && ALL_CHANGE_KINDS.every(kind => changeKinds.includes(kind))
}

export function changeKindSelectionKey(changeKinds: readonly ChangeKind[]) {
  return [...changeKinds].sort().join(',')
}
