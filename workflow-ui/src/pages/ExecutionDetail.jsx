import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api/client'
import './ExecutionDetail.css'

export default function ExecutionDetail() {
  const { id } = useParams()
  const [exec, setExec] = useState(null)
  const [workflow, setWorkflow] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actioning, setActioning] = useState(false)

  useEffect(() => {
    api.executions.get(id)
      .then((e) => {
        setExec(e)
        if (e.workflowId) {
          return api.workflows.get(e.workflowId).then(setWorkflow)
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
        <h2>Execution logs</h2>
        {logs.length === 0 ? (
          <p className="text-muted">No step logs yet.</p>
        ) : (
          <ul className="log-list">
            {logs.map((entry, idx) => (
              <li key={idx} className="log-entry">
                <div className="log-header">
                  <span className="log-step-name">{entry.step_name ?? 'Step'}</span>
                  <span className="badge step-type">{entry.step_type}</span>
                  <span className="log-status">{entry.status}</span>
                </div>
                {entry.selected_next_step != null && (
                  <div className="log-next">Next: {entry.selected_next_step}</div>
                )}
                {entry.evaluated_rules && entry.evaluated_rules.length > 0 && (
                  <div className="log-rules">
                    <strong>Rules evaluated:</strong>
                    <ul>
                      {entry.evaluated_rules.map((r, i) => (
                        <li key={i}>
                          <code>{r.rule}</code> → {r.result ? 'true' : 'false'}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {entry.error_message && (
                  <div className="log-error">{entry.error_message}</div>
                )}
                {entry.started_at && (
                  <div className="log-time">
                    {entry.started_at} → {entry.ended_at}
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
