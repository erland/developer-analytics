import * as React from 'react'
import { useGitHubDisconnect, type DisconnectDataDisposition } from '../hooks/useGitHubDisconnect'
import { useSyncRecovery } from '../hooks/useSyncRecovery'

export function PrivacyDataSourcesView() {
  const disconnect = useGitHubDisconnect()
  const recovery = useSyncRecovery()
  const [disconnectDisposition, setDisconnectDisposition] =
    React.useState<DisconnectDataDisposition | null>(null)

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Privacy and connected data</p>
          <h2>Privacy/data sources</h2>
        </div>
      </div>

      <section className="dashboard-section">
        <span className="card-kicker">GitHub App</span>
        <h2>Repository access</h2>
        <p className="settings-intro">
          GitHub controls which repositories Developer Analytics can access through the
          GitHub App installation. Developer Analytics analyses every repository exposed
          by that installation and does not maintain a second repository selection.
        </p>

        <div className="permission-card">
          <div>
            <strong>Private repositories</strong>
            <span>Managed by your GitHub App installation</span>
          </div>
          <p>
            To include a private repository in Developer Analytics, install the GitHub App
            for that repository or add it to the existing GitHub App installation in GitHub.
            Removing a repository from the installation prevents future synchronisation of it.
          </p>
        </div>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Operational recovery</span>
        <h2>Synchronisation recovery</h2>
        <p className="settings-intro">
          Interrupted worker jobs recover automatically. You can also request
          recovery or queue a fresh GitHub synchronisation after changing the
          GitHub App installation or after a temporary error.
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
        <h2>GitHub is the source of truth</h2>
        <p>
          Repository access is determined by the GitHub App installation. Developer
          Analytics only synchronises repositories that GitHub exposes to the app and
          does not ask you to repeat that repository selection here.
        </p>
      </section>
    </>
  )
}
