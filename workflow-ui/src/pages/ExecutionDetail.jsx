import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api/client'
import './ExecutionDetail.css'

function formatDurationMs(ms) {
  if (!Number.isFinite(ms) || ms < 0) return null
  const totalSeconds = Math.floor(ms / 1000)
  const hh = String(Math.floor(totalSeconds / 3600)).padStart(2, '0')
  const mm = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0')
  const ss = String(totalSeconds % 60).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

function entryDuration(entry) {
  try {
    if (!entry?.started_at || !entry?.ended_at) return null
    const a = new Date(entry.started_at).getTime()
    const b = new Date(entry.ended_at).getTime()
    if (!Number.isFinite(a) || !Number.isFinite(b)) return null
    return formatDurationMs(b - a)
  } catch {
    return null
  }
}

export default function ExecutionDetail() {
  const { id } = useParams()
  const [exec, setExec] = useState(null)
  const [workflow, setWorkflow] = useState(null)
  const [steps, setSteps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actioning, setActioning] = useState(false)

  useEffect(() => {
    api.executions.get(id)
      .then((e) => {
        setExec(e)
        if (e.workflowId) {
          return api.workflows.getDetail(e.workflowId).then((d) => {
            setWorkflow(d.workflow)
            setSteps(d.steps || [])
          })
        }
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  const handleCancel = () => {
    setActioning(true)
    api.executions.cancel(id)
      .then(setExec)
      .catch((e) => setError(e.message))
      .finally(() => setActioning(false))
  }

  const handleRetry = () => {
    setActioning(true)
    api.executions.retry(id)
      .then(setExec)
      .catch((e) => setError(e.message))
      .finally(() => setActioning(false))
  }

  if (loading) return <div className="loading">Loading…</div>
  if (!exec) return <div className="error-banner">Execution not found</div>

  const statusClass = `badge badge-${(exec.status || '').toLowerCase().replace(' ', '_')}`
  const logs = exec.logs || []
  const stepNameById = new Map(steps.map((s) => [s.id, s.name]))
  const currentStepName = exec.currentStepId ? (stepNameById.get(exec.currentStepId) ?? exec.currentStepId) : null
  const lastStepName = logs.length > 0 ? (logs[logs.length - 1]?.step_name ?? null) : null

  return (
    <div className="execution-detail">
      <div className="editor-header">
        <h1 className="page-title">Execution {String(exec.id).slice(0, 8)}…</h1>
        <div className="action-buttons">
          <Link to="/audit" className="btn btn-secondary">Audit log</Link>
          <Link to="/" className="btn btn-secondary">Workflows</Link>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card section summary">
        <h2>Summary</h2>
        <dl className="summary-grid">
          <dt>Status</dt>
          <dd><span className={statusClass}>{exec.status}</span></dd>
          <dt>Workflow</dt>
          <dd>{workflow?.name ?? exec.workflowId}</dd>
          <dt>Version</dt>
          <dd>{exec.workflowVersion}</dd>
          <dt>Current step</dt>
          <dd>{currentStepName ?? (lastStepName ? `${lastStepName} (last)` : '—')}</dd>
          <dt>Started</dt>
          <dd>{exec.startedAt ? new Date(exec.startedAt).toLocaleString() : '—'}</dd>
          <dt>Ended</dt>
          <dd>{exec.endedAt ? new Date(exec.endedAt).toLocaleString() : '—'}</dd>
          <dt>Retries</dt>
          <dd>{exec.retries ?? 0}</dd>
        </dl>
        {(exec.status === 'in_progress' || exec.status === 'pending') && (
          <button type="button" className="btn btn-danger" onClick={handleCancel} disabled={actioning}>
            Cancel
          </button>
        )}
        {exec.status === 'failed' && (
          <button type="button" className="btn btn-primary" onClick={handleRetry} disabled={actioning}>
            Retry failed step
          </button>
        )}
      </div>

      {exec.data && Object.keys(exec.data).length > 0 && (
        <div className="card section">
          <h2>Input data</h2>
          <pre className="data-json">{JSON.stringify(exec.data, null, 2)}</pre>
        </div>
      )}

      <div className="card section">
        <h2>Execution progress / logs</h2>
        {logs.length === 0 ? (
          <p className="text-muted">No step logs yet.</p>
        ) : (
          <ul className="log-list">
            {logs.map((entry, idx) => (
              <li key={idx} className="log-entry">
                <div className="log-header">
                  <span className="log-step-name">[Step {idx + 1}] {entry.step_name ?? 'Step'}</span>
                  <span className="badge step-type">{entry.step_type}</span>
                  <span className="log-status">{entry.status}</span>
                </div>
                {entry.selected_next_step != null && (
                  <div className="log-next">Next: {entry.selected_next_step}</div>
                )}
                {entry.evaluated_rules && entry.evaluated_rules.length > 0 && (
                  <div className="log-rules">
                    <strong>Rules evaluated:</strong>
                    <ul className="rule-evals">
                      {entry.evaluated_rules.map((r, i) => (
                        <li key={i}>
                          <code>{r.rule}</code> → {r.result ? 'true' : 'false'}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {entry.approver_id && (
                  <div className="log-meta">Approver: <code>{entry.approver_id}</code></div>
                )}
                {entry.error_message && (
                  <div className="log-error">{entry.error_message}</div>
                )}
                {entry.started_at && (
                  <div className="log-time">
                    <span>{entry.started_at} → {entry.ended_at}</span>
                    {entryDuration(entry) && <span className="log-duration">Duration: {entryDuration(entry)}</span>}
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
