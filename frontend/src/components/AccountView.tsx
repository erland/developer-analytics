import { useState } from 'react'
import {
  externalClientScopes,
  externalPrivacyScopes,
  type ExternalClientScope,
  type ExternalPrivacyScope,
  useExternalClients,
} from '../hooks/useExternalClients'
import { useUserDataDeletion } from '../hooks/useUserDataDeletion'

export function AccountView() {
  const deletion = useUserDataDeletion()
  const [deleteConfirmation, setDeleteConfirmation] = useState('')
  const external = useExternalClients()
  const [name, setName] = useState('ChatGPT')
  const [privacyScope, setPrivacyScope] =
    useState<ExternalPrivacyScope>('PUBLIC_ONLY')
  const [scopes, setScopes] = useState<ExternalClientScope[]>([
    'PROFILE_READ',
    'PROJECTS_READ',
    'ACTIVITY_READ',
    'TECHNOLOGIES_READ',
    'PROJECT_TYPES_READ',
    'CONTRIBUTIONS_READ',
    'EVIDENCE_READ',
    'AI_ASSESSMENTS_WRITE',
  ])

  function toggle(scope: ExternalClientScope) {
    setScopes((current) =>
      current.includes(scope)
        ? current.filter((item) => item !== scope)
        : [...current, scope],
    )
  }

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">User-controlled access</p>
          <h2>Account</h2>
        </div>
      </div>

      <section className="dashboard-section">
        <span className="card-kicker">External clients</span>
        <h2>GPT/API access tokens</h2>
        <p className="settings-intro">
          These credentials are separate from your browser session and GitHub
          connection. Each token is user-specific, scoped and revocable.
        </p>

        <label className="account-field">
          <span>Client name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} />
        </label>

        <fieldset className="external-scope-grid">
          <legend>Allowed reads</legend>
          {externalClientScopes.map((scope) => (
            <label key={scope}>
              <input
                type="checkbox"
                checked={scopes.includes(scope)}
                onChange={() => toggle(scope)}
              />
              <span>{scope.toLowerCase().replaceAll('_', ' ')}</span>
            </label>
          ))}
        </fieldset>

        <fieldset className="external-scope-grid">
          <legend>Privacy scope</legend>
          {externalPrivacyScopes.map((scope) => (
            <label key={scope}>
              <input
                type="radio"
                name="external-privacy-scope"
                checked={privacyScope === scope}
                onChange={() => setPrivacyScope(scope)}
              />
              <span>
                {scope === 'PUBLIC_ONLY'
                  ? 'Public data only'
                  : scope === 'PUBLIC_PLUS_PRIVATE_AGGREGATES'
                    ? 'Public + private aggregates'
                    : 'Full authorised analysis'}
              </span>
            </label>
          ))}
        </fieldset>

        <button
          type="button"
          className="primary-export-action"
          disabled={!name.trim() || scopes.length === 0 || external.status === 'saving'}
          onClick={() => void external.create(name, scopes, privacyScope)}
        >
          Create external client token
        </button>

        {external.createdToken ? (
          <div className="one-time-token" role="status">
            <strong>Copy this token now</strong>
            <code>{external.createdToken}</code>
            <span>It is shown once and cannot be retrieved later.</span>
          </div>
        ) : null}

        {external.error ? (
          <p className="settings-error" role="alert">{external.error}</p>
        ) : null}
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Existing credentials</span>
        <h2>External clients</h2>
        {external.clients.length ? (
          <div className="project-list">
            {external.clients.map((client) => (
              <article className="project-row" key={client.id}>
                <div>
                  <h3>{client.name}</h3>
                  <p>
                    {client.scopes.length} scopes · {client.privacyScope
                      .toLowerCase()
                      .replaceAll('_', ' ')} · created{' '}
                    {new Date(client.createdAt).toLocaleDateString()}
                    {client.revokedAt ? ' · revoked' : ''}
                  </p>
                </div>
                {!client.revokedAt ? (
                  <button
                    type="button"
                    className="secondary-action"
                    onClick={() => void external.revoke(client.id)}
                  >
                    Revoke
                  </button>
                ) : null}
              </article>
            ))}
          </div>
        ) : (
          <p className="empty-state">No external client tokens yet.</p>
        )}
      </section>

<section className="dashboard-section destructive-zone">
  <span className="card-kicker">Data lifecycle</span>
  <h2>Delete all Developer Analytics data</h2>
  <p className="settings-intro">
    This permanently removes your Developer Analytics account data,
    including provider connections, repositories, contributions,
    aggregates, evidence, classifications, AI assessments and background
    jobs. Generated Markdown/PDF files are not stored server-side.
  </p>

  <label className="account-field">
    <span>Type DELETE_MY_DATA to confirm</span>
    <input
      value={deleteConfirmation}
      onChange={(event) => setDeleteConfirmation(event.target.value)}
      autoComplete="off"
    />
  </label>

  {deletion.error ? (
    <p className="settings-error" role="alert">{deletion.error}</p>
  ) : null}

  <button
    type="button"
    className="destructive-action"
    disabled={
      deleteConfirmation !== 'DELETE_MY_DATA' ||
      deletion.status === 'deleting'
    }
    onClick={() => void deletion.deleteAllData()}
  >
    {deletion.status === 'deleting'
      ? 'Deleting…'
      : 'Permanently delete my data'}
  </button>
</section>
    </>
  )
}
