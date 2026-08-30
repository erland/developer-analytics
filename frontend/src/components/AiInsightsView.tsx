import { useAiStatus } from '../hooks/useAiStatus'
import { type AiPrivacyPolicy, useAiPrivacy } from '../hooks/useAiPrivacy'
import { useUserAiInsights } from '../hooks/useUserAiInsights'

export function AiInsightsView() {
  const ai = useAiStatus()
  const privacy = useAiPrivacy()
  const insights = useUserAiInsights()

  async function choose(policy: AiPrivacyPolicy) {
    await privacy.update(policy)
  }

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Optional analysis layer</p>
          <h2>AI insights</h2>
        </div>
      </div>

      <section className="dashboard-section">
        <span className="card-kicker">AI availability</span>
        <h2>
          {ai.status === 'ready' && ai.configured
            ? 'AI-assisted analysis available'
            : 'Deterministic analytics remain primary'}
        </h2>
        {ai.status === 'loading' ? (
          <p className="empty-state">Checking AI availability…</p>
        ) : (
          <p className="settings-intro">{ai.message}</p>
        )}
      </section>


<section className="dashboard-section">
  <div className="ai-insight-heading">
    <div>
      <span className="card-kicker">AI-generated</span>
      <h2>User-level insights</h2>
    </div>
    <button
      className="secondary-action"
      type="button"
      disabled={!ai.configured || insights.status === 'generating'}
      onClick={() => void insights.generate()}
    >
      {insights.status === 'generating'
        ? 'Generating…'
        : insights.insight
          ? 'Refresh AI insights'
          : 'Generate AI insights'}
    </button>
  </div>

  {!ai.configured ? (
    <p className="settings-intro">
      Configure an AI provider to generate this optional layer.
    </p>
  ) : null}

  {insights.error ? (
    <p className="settings-error" role="alert">{insights.error}</p>
  ) : null}

  {insights.insight ? (
    <div className="ai-insight-grid">
      <article className="ai-insight-card">
        <span>Likely roles</span>
        <div className="ai-role-list">
          {insights.insight.likelyRoles.map((role) => (
            <div key={role.role}>
              <strong>{role.role}</strong>
              <small>
                {Math.round(role.confidence * 100)}% confidence · {role.rationale}
              </small>
            </div>
          ))}
        </div>
      </article>

      <article className="ai-insight-card">
        <span>Technical focus</span>
        <p>{insights.insight.technicalFocus}</p>
      </article>

      <article className="ai-insight-card">
        <span>Breadth and depth</span>
        <p>{insights.insight.breadthDepthObservation}</p>
      </article>

      <article className="ai-insight-card">
        <span>Technology evolution</span>
        <p>{insights.insight.technologyEvolutionSummary}</p>
      </article>

      <article className="ai-insight-card">
        <span>Open-source engagement</span>
        <p>{insights.insight.openSourceEngagementSummary}</p>
      </article>

      <p className="ai-generated-note">
        AI-generated interpretation · {insights.insight.providerId ?? 'provider'} /
        {insights.insight.modelId ?? 'model'} · {insights.insight.privacyProvenance}
      </p>
    </div>
  ) : null}
</section>

      <section className="dashboard-section">
        <span className="card-kicker">AI privacy</span>
        <h2>Private repository data</h2>
        <p className="settings-intro">
          Private repository content is never sent to the external AI provider
          in the current policy model. You may separately decide whether
          private metadata may be used.
        </p>

        {privacy.status === 'loading' ? (
          <p className="empty-state">Loading AI privacy settings…</p>
        ) : (
          <fieldset className="export-option-group">
            <legend>Allowed AI data</legend>

            <label className="export-option">
              <input
                type="radio"
                name="ai-privacy"
                checked={privacy.policy === 'PRIVATE_AI_DISABLED'}
                onChange={() => void choose('PRIVATE_AI_DISABLED')}
              />
              <span>
                <strong>Disable AI for private data</strong>
                <small>Safest default. Private metadata and content stay out of AI requests.</small>
              </span>
            </label>

            <label className="export-option">
              <input
                type="radio"
                name="ai-privacy"
                checked={privacy.policy === 'PUBLIC_ONLY'}
                onChange={() => void choose('PUBLIC_ONLY')}
              />
              <span>
                <strong>Public data only</strong>
                <small>AI may use public repository evidence only.</small>
              </span>
            </label>

            <label className="export-option">
              <input
                type="radio"
                name="ai-privacy"
                checked={privacy.policy === 'PRIVATE_METADATA_ALLOWED'}
                onChange={() => void choose('PRIVATE_METADATA_ALLOWED')}
              />
              <span>
                <strong>Allow private metadata</strong>
                <small>
                  Private repository metadata may be used only if the configured
                  provider policy also permits it. Private source content remains blocked.
                </small>
              </span>
            </label>
          </fieldset>
        )}

        {privacy.error ? (
          <p className="settings-error" role="alert">{privacy.error}</p>
        ) : null}
      </section>

      <section className="dashboard-section privacy-note">
        <span className="card-kicker">Enforced boundary</span>
        <h2>Consent cannot widen provider policy</h2>
        <p>
          Every AI call carries an explicit data-sensitivity context. The backend
          checks source sensitivity, your consent and the configured provider
          policy before calling Gemini. A denied request produces no external AI call.
        </p>
      </section>
    </>
  )
}
