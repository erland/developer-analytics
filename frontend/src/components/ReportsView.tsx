import { useState } from 'react'

type OutputFormat = 'MARKDOWN' | 'PDF'

type ReportType =
  | 'PUBLIC_OSS_REPORT'
  | 'FULL_DEVELOPER_REPORT'
  | 'TECHNOLOGY_PROFILE'
  | 'ACTIVITY_REPORT'

type PrivateDataMode =
  | 'EXCLUDE_PRIVATE'
  | 'INCLUDE_PRIVATE_AGGREGATES'
  | 'INCLUDE_FULL_PRIVATE_DETAIL'

type PrivateNameMode = 'SHOW' | 'HIDE'

type ReportPreview = {
  reportType: ReportType
  privateDataMode: PrivateDataMode
  privacyScope:
    | 'PUBLIC_ONLY'
    | 'PUBLIC_PLUS_PRIVATE_AGGREGATES'
    | 'FULL_PRIVATE_DETAIL'
  privateRepositoriesIncluded: boolean
  privateNamesIncluded: boolean
  aiAssessmentsIncluded: boolean
  firstActivityAt: string | null
  lastActivityAt: string | null
  repositoryCount: number
  publicRepositoryCount: number
  privateRepositoryCount: number
  contributionCount: number
  reportModelVersion: string
}

export function ReportsView() {
  const [outputFormat, setOutputFormat] = useState<OutputFormat>('MARKDOWN')
  const [reportType, setReportType] = useState<ReportType | null>(null)
  const [privateDataMode, setPrivateDataMode] =
    useState<PrivateDataMode | null>(null)
  const [privateNameMode, setPrivateNameMode] =
    useState<PrivateNameMode | null>(null)
  const [preview, setPreview] = useState<ReportPreview | null>(null)
  const [status, setStatus] =
    useState<'idle' | 'previewing' | 'exporting' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  const ready =
    reportType !== null && privateDataMode !== null && privateNameMode !== null

  function changeReportType(value: ReportType) {
    setReportType(value)
    setPreview(null)
  }

  function changePrivateDataMode(value: PrivateDataMode) {
    setPrivateDataMode(value)
    setPreview(null)
  }

  function changePrivateNameMode(value: PrivateNameMode) {
    setPrivateNameMode(value)
    setPreview(null)
  }

  function requestBody() {
    return {
      outputFormat,
      reportType,
      privateDataMode,
      hidePrivateRepositoryNames: privateNameMode === 'HIDE',
    }
  }

  async function previewReport() {
    if (!ready) return

    setStatus('previewing')
    setError(null)
    setPreview(null)

    try {
      const response = await fetch('/api/me/reports/preview', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody()),
      })

      if (!response.ok) {
        throw new Error(`Preview failed with HTTP ${response.status}`)
      }

      setPreview((await response.json()) as ReportPreview)
      setStatus('idle')
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to preview report',
      )
    }
  }

  async function exportReport() {
    if (!ready || !preview) return

    setStatus('exporting')
    setError(null)

    try {
      const response = await fetch('/api/me/reports/export', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: outputFormat === 'PDF' ? 'application/pdf' : 'text/markdown',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...requestBody(),
          generationConfirmed: true,
        }),
      })

      if (!response.ok) {
        throw new Error(`Export failed with HTTP ${response.status}`)
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      const disposition = response.headers.get('Content-Disposition')
      const filename = disposition?.match(/filename="([^"]+)"/)?.[1]
      link.download =
        filename ??
        (outputFormat === 'PDF'
          ? 'developer-analytics-export.pdf'
          : 'developer-analytics-export.md')
      link.click()
      URL.revokeObjectURL(url)
      setStatus('idle')
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to export report',
      )
    }
  }

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Controlled export</p>
          <h2>Reports</h2>
        </div>
      </div>

      <section className="dashboard-section">
        <span className="card-kicker">Output format</span>
        <h2>Choose export format</h2>
        <fieldset className="export-option-group">
          <legend>Format</legend>
          <label className="export-option">
            <input
              type="radio"
              name="output-format"
              checked={outputFormat === 'MARKDOWN'}
              onChange={() => {
                setOutputFormat('MARKDOWN')
                setPreview(null)
              }}
            />
            <span>
              <strong>Markdown</strong>
              <small>Portable canonical text export.</small>
            </span>
          </label>
          <label className="export-option">
            <input
              type="radio"
              name="output-format"
              checked={outputFormat === 'PDF'}
              onChange={() => {
                setOutputFormat('PDF')
                setPreview(null)
              }}
            />
            <span>
              <strong>PDF</strong>
              <small>A4 print layout rendered from the same canonical report model.</small>
            </span>
          </label>
        </fieldset>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Report content</span>
        <h2>Choose report type</h2>
        <p className="settings-intro">
          All variants use the same canonical report model. Preview is required
          before the separate generation action becomes available.
        </p>

        <fieldset className="export-option-group">
          <legend>Report type</legend>
          {([
            ['PUBLIC_OSS_REPORT', 'Public OSS report', 'Public repositories only; private data is always excluded server-side.'],
            ['FULL_DEVELOPER_REPORT', 'Full developer report', 'Broad report covering projects, technologies, activity and optional AI assessment.'],
            ['TECHNOLOGY_PROFILE', 'Technology profile', 'Focused technology evidence and optional AI interpretation.'],
            ['ACTIVITY_REPORT', 'Activity report', 'Contribution totals and monthly activity over the available period.'],
          ] as const).map(([value, title, description]) => (
            <label className="export-option" key={value}>
              <input
                type="radio"
                name="report-type"
                checked={reportType === value}
                onChange={() => changeReportType(value)}
              />
              <span>
                <strong>{title}</strong>
                <small>{description}</small>
              </span>
            </label>
          ))}
        </fieldset>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Export privacy</span>
        <h2>Choose what may leave the private dashboard</h2>
        <p className="settings-intro">
          Every export requires explicit privacy choices. Changing any choice
          invalidates the previous preview.
        </p>

        <fieldset className="export-option-group">
          <legend>Private data</legend>
          <label className="export-option">
            <input
              type="radio"
              name="private-data-mode"
              checked={privateDataMode === 'EXCLUDE_PRIVATE'}
              onChange={() => changePrivateDataMode('EXCLUDE_PRIVATE')}
            />
            <span>
              <strong>Exclude private data</strong>
              <small>Export only analysis based on public repositories.</small>
            </span>
          </label>
          <label className="export-option">
            <input
              type="radio"
              name="private-data-mode"
              checked={privateDataMode === 'INCLUDE_PRIVATE_AGGREGATES'}
              onChange={() =>
                changePrivateDataMode('INCLUDE_PRIVATE_AGGREGATES')
              }
            />
            <span>
              <strong>Include aggregated private data</strong>
              <small>Include private totals without per-project private detail.</small>
            </span>
          </label>
          <label className="export-option">
            <input
              type="radio"
              name="private-data-mode"
              checked={privateDataMode === 'INCLUDE_FULL_PRIVATE_DETAIL'}
              onChange={() =>
                changePrivateDataMode('INCLUDE_FULL_PRIVATE_DETAIL')
              }
            />
            <span>
              <strong>Include full private project detail</strong>
              <small>Include private repositories and project-level statistics.</small>
            </span>
          </label>
        </fieldset>

        <fieldset className="export-option-group">
          <legend>Private repository names</legend>
          <label className="export-option">
            <input
              type="radio"
              name="private-name-mode"
              checked={privateNameMode === 'HIDE'}
              onChange={() => changePrivateNameMode('HIDE')}
            />
            <span>
              <strong>Hide private repository names</strong>
              <small>Private projects are labelled generically when detail is included.</small>
            </span>
          </label>
          <label className="export-option">
            <input
              type="radio"
              name="private-name-mode"
              checked={privateNameMode === 'SHOW'}
              onChange={() => changePrivateNameMode('SHOW')}
            />
            <span>
              <strong>Show private repository names</strong>
              <small>Preserve names when full private detail is exported.</small>
            </span>
          </label>
        </fieldset>

        {!ready ? (
          <p className="export-requirement" role="status">
            Select report type and both export privacy settings to preview.
          </p>
        ) : null}

        {status === 'error' ? (
          <p className="settings-error" role="alert">{error}</p>
        ) : null}

        <button
          className="secondary-action"
          type="button"
          disabled={!ready || status === 'previewing' || status === 'exporting'}
          onClick={() => void previewReport()}
        >
          {status === 'previewing' ? 'Preparing preview…' : 'Preview report privacy'}
        </button>
      </section>

      {preview ? (
        <section className="dashboard-section report-privacy-preview">
          <span className="card-kicker">Privacy preview</span>
          <h2>Review before generation</h2>

          <dl className="privacy-preview-grid">
            <div>
              <dt>Private repositories included?</dt>
              <dd>{yesNo(preview.privateRepositoriesIncluded)}</dd>
            </div>
            <div>
              <dt>Private names included?</dt>
              <dd>{yesNo(preview.privateNamesIncluded)}</dd>
            </div>
            <div>
              <dt>AI assessments included?</dt>
              <dd>{yesNo(preview.aiAssessmentsIncluded)}</dd>
            </div>
            <div>
              <dt>Data scope</dt>
              <dd>{privacyLabel(preview.privacyScope)}</dd>
            </div>
            <div>
              <dt>Analysed time range</dt>
              <dd>{periodLabel(preview.firstActivityAt, preview.lastActivityAt)}</dd>
            </div>
            <div>
              <dt>Coverage</dt>
              <dd>
                {preview.repositoryCount} repositories · {preview.contributionCount}{' '}
                contributions
              </dd>
            </div>
          </dl>

          <p className="settings-intro">
            This preview reflects the effective server-side scope. For example,
            Public OSS reports remain public-only even if a broader private option
            was selected above.
          </p>

          <button
            className="primary-export-action"
            type="button"
            disabled={status === 'exporting'}
            onClick={() => void exportReport()}
          >
            {status === 'exporting'
              ? 'Generating report…'
              : outputFormat === 'PDF' ? 'Generate PDF report' : 'Generate Markdown report'}
          </button>
        </section>
      ) : null}

      <section className="dashboard-section privacy-note">
        <span className="card-kicker">Export boundary</span>
        <h2>Preview, then explicitly generate</h2>
        <p>
          No report file is generated by the preview action. Generation requires
          a separate button after the effective privacy scope has been shown.
        </p>
      </section>
    </>
  )
}

function yesNo(value: boolean) {
  return value ? 'Yes' : 'No'
}

function privacyLabel(value: ReportPreview['privacyScope']) {
  if (value === 'FULL_PRIVATE_DETAIL') return 'Private scope — full authorised detail'
  if (value === 'PUBLIC_PLUS_PRIVATE_AGGREGATES') {
    return 'Private scope — aggregates only'
  }
  return 'Public only'
}

function periodLabel(first: string | null, last: string | null) {
  if (!first || !last) return 'No recorded activity'
  const format = new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
  return `${format.format(new Date(first))} – ${format.format(new Date(last))}`
}
