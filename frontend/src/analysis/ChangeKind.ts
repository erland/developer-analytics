export type ChangeKind = 'CODE' | 'DOCUMENTATION' | 'CI_CD' | 'OTHER'

export const ALL_CHANGE_KINDS: readonly ChangeKind[] = ['CODE', 'DOCUMENTATION', 'CI_CD', 'OTHER']

export function normalizeChangeKinds(changeKinds?: readonly ChangeKind[]) {
  return changeKinds && changeKinds.length > 0 ? changeKinds : ALL_CHANGE_KINDS
}

export function appendChangeKinds(params: URLSearchParams, changeKinds?: readonly ChangeKind[]) {
  const effective = normalizeChangeKinds(changeKinds)
  if (isAllChangeKinds(effective)) return params
  params.set('changeKinds', effective.join(','))
  return params
}

export function isAllChangeKinds(changeKinds?: readonly ChangeKind[]) {
  const effective = normalizeChangeKinds(changeKinds)
  return effective.length === ALL_CHANGE_KINDS.length && ALL_CHANGE_KINDS.every(kind => effective.includes(kind))
}

export function changeKindSelectionKey(changeKinds?: readonly ChangeKind[]) {
  return [...normalizeChangeKinds(changeKinds)].sort().join(',')
}
