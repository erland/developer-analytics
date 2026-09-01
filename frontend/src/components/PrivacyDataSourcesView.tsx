import * as React from 'react'
import { useGitHubDataSource } from '../hooks/useGitHubDataSource'
import { usePrivateRepositories } from '../hooks/usePrivateRepositories'
import { useGitHubDisconnect, type DisconnectDataDisposition } from '../hooks/useGitHubDisconnect'
import { useSyncRecovery } from '../hooks/useSyncRecovery'

export function PrivacyDataSourcesView() {
  const disconnect = useGitHubDisconnect()
  const recovery = useSyncRecovery()
  const [privatePrefix, setPrivatePrefix] = React.useState('')
  const [disconnectDisposition, setDisconnectDisposition] =
    React.useState<DisconnectDataDisposition | null>(null)
  const github = useGitHubDataSource()
  const privateRepos = usePrivateRepositories(github.status === 'ready' && github.privateRepositoriesAuthorised)

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Privacy and connected data</p>
          <h2>Privacy/data sources</h2>
        </div>
      </div>

      <section className="dashboard-section">
        <span className="card-kicker">GitHub</span>
        <h2>Repository access</h2>
        <p className="settings-intro">
          Public repository analysis is available without granting Developer Analytics
          access to private repositories. Private access is optional and is never enabled
          automatically.
        </p>

        {github.status === 'loading' ? (
          <p className="empty-state">Loading GitHub access settings…</p>
        ) : null}

        {github.status === 'error' ? (
          <p className="settings-error">{github.error}</p>
        ) : null}

        {github.status === 'ready' ? (
          <div className="permission-card">
            <div>
              <strong>Private repositories</strong>
              <span>
                {github.privateRepositoriesAuthorised
                  ? 'Authorised for analysis'
                  : 'Not authorised'}
              </span>
            </div>

            {github.privateRepositoriesAuthorised ? (
              <>
                <p>
                  Developer Analytics may include private repositories returned by GitHub
                  in your private analysis. Private repository access remains separate from
                  the public-data analysis.
                </p>
                <form
                  method="post"
                  action="/api/auth/github/private-repositories/remove"
                >
                  <button className="secondary-action" type="submit">
                    Stop using private repositories
                  </button>
                </form>
              </>
            ) : (
              <>
                <p>
                  Authorising this will ask GitHub for additional repository permission.
                  Nothing changes until you explicitly continue to GitHub and approve it.
                </p>
                <a
                  className="primary-action-link"
                  href="/api/auth/github/private-repositories/authorise"
                >
                  Authorise private repositories
                </a>
              </>
            )}
          </div>
        ) : null}
      </section>


{github.status === 'ready' && github.privateRepositoriesAuthorised ? (
  <section className="dashboard-section">
    <div className="private-repository-header">
      <div>
        <span className="card-kicker">Private repository selection</span>
        <h2>Repositories included in analysis</h2>
      </div>
      <button
        className="secondary-action"
        type="button"
        disabled={privateRepos.refreshing}
        onClick={() => void privateRepos.refresh()}
      >
        {privateRepos.refreshing ? 'Refreshing…' : 'Refresh permissions'}
      </button>
    </div>
    <p className="settings-intro">
      GitHub may authorise several private repositories. Only repositories explicitly
      selected here are included in Developer Analytics statistics and assessments.
    </p>
    <div className="recovery-actions">
      <button
        className="secondary-action"
        type="button"
        onClick={() => void privateRepos.bulkSetIncluded(true)}
      >
        Include all private repositories
      </button>
      <button
        className="secondary-action"
        type="button"
        onClick={() => void privateRepos.bulkSetIncluded(false)}
      >
        Exclude all private repositories
      </button>
    </div>
    <div className="view-toolbar">
      <label>
        <span>Repository prefix</span>
        <input
          type="text"
          value={privatePrefix}
          placeholder="e.g. customer- or erland/project-"
          onChange={(event) => setPrivatePrefix(event.target.value)}
        />
      </label>
      <button
        className="secondary-action"
        type="button"
        disabled={!privatePrefix.trim()}
        onClick={() => void privateRepos.bulkSetIncluded(true, privatePrefix)}
      >
        Include matching prefix
      </button>
      <button
        className="text-button"
        type="button"
        disabled={!privatePrefix.trim()}
        onClick={() => void privateRepos.bulkSetIncluded(false, privatePrefix)}
      >
        Exclude matching prefix
      </button>
    </div>
    {privateRepos.loading ? <p className="empty-state">Loading private repositories…</p> : null}
    {privateRepos.error ? <p className="settings-error">{privateRepos.error}</p> : null}
    {!privateRepos.loading && privateRepos.repositories.length === 0 ? (
      <p className="empty-state">No authorised private repositories have been discovered yet.</p>
    ) : null}
    <div className="private-repository-list">
      {privateRepos.repositories.map((repository) => (
        <article className="private-repository-row" key={repository.id}>
          <div>
            <strong>{repository.fullName || repository.name}</strong>
            <span>{repository.syncStatus.toLowerCase()}</span>
          </div>
          <label className="repository-selection-toggle">
            <input
              type="checkbox"
              checked={repository.includedInAnalysis}
              onChange={(event) => void privateRepos.setIncluded(repository.id, event.target.checked)}
            />
            <span>Include in analysis</span>
          </label>
          {repository.includedInAnalysis ? (
            <button className="text-button" type="button" onClick={() => void privateRepos.remove(repository.id)}>
              Remove from analysis
            </button>
          ) : null}
        </article>
      ))}
    </div>
  </section>
) : null}



<section className="dashboard-section">
  <span className="card-kicker">Operational recovery</span>
  <h2>Synchronisation recovery</h2>
  <p className="settings-intro">
    Interrupted worker jobs recover automatically. You can also request
    recovery or queue a fresh GitHub synchronisation after a temporary error.
  </p>
  <div className="recovery-actions">
    <button
      className="secondary-action"
      type="button"
      disabled={recovery.status !== 'idle'}
      onClick={() => void recovery.recoverInterrupted()}
    >
      Recover interrupted jobs
    </button>
    <button
      className="secondary-action"
      type="button"
      disabled={recovery.status !== 'idle'}
      onClick={() => void recovery.retryGitHub()}
    >
      Retry GitHub synchronisation
    </button>
  </div>
  {recovery.message ? (
    <p className="settings-intro" role="status">{recovery.message}</p>
  ) : null}
</section>

<section className="dashboard-section disconnect-zone">
  <span className="card-kicker">Connection lifecycle</span>
  <h2>Disconnect GitHub</h2>
  <p className="settings-intro">
    Disconnecting immediately removes the stored GitHub access credential
    and stops future queued synchronisation. Choose explicitly what should
    happen to data Developer Analytics has already analysed.
  </p>

  <fieldset className="export-option-group">
    <legend>Existing analysed data</legend>
    <label className="export-option">
      <input
        type="radio"
        name="disconnect-data-disposition"
        checked={disconnectDisposition === 'PRESERVE_ANALYSED_DATA'}
        onChange={() => setDisconnectDisposition('PRESERVE_ANALYSED_DATA')}
      />
      <span>
        <strong>Preserve analysed data</strong>
        <small>
          Keep the existing dashboard history and derived analysis, but do
          not synchronise GitHub again until reconnected.
        </small>
      </span>
    </label>
    <label className="export-option">
      <input
        type="radio"
        name="disconnect-data-disposition"
        checked={disconnectDisposition === 'REMOVE_ANALYSED_DATA'}
        onChange={() => setDisconnectDisposition('REMOVE_ANALYSED_DATA')}
      />
      <span>
        <strong>Remove analysed GitHub data</strong>
        <small>
          Delete collected repositories and their derived analysis for this
          account. This cannot be reconstructed without reconnecting.
        </small>
      </span>
    </label>
  </fieldset>

  {disconnect.error ? (
    <p className="settings-error" role="alert">{disconnect.error}</p>
  ) : null}

  <button
    className="secondary-action"
    type="button"
    disabled={
      disconnectDisposition === null ||
      disconnect.status === 'disconnecting'
    }
    onClick={() =>
      disconnectDisposition
        ? void disconnect.disconnect(disconnectDisposition)
        : undefined
    }
  >
    {disconnect.status === 'disconnecting'
      ? 'Disconnecting…'
      : 'Disconnect GitHub'}
  </button>
</section>

      <section className="dashboard-section privacy-note">
        <span className="card-kicker">Permission boundary</span>
        <h2>Explicit by design</h2>
        <p>
          Developer Analytics does not treat possession of a broader GitHub token as consent.
          Private repositories are excluded by the backend unless this setting has been
          explicitly authorised.
        </p>
      </section>
    </>
  )
}
