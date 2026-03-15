import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import './AuditLog.css'

export default function AuditLog() {
  const [page, setPage] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [currentPage, setCurrentPage] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    api.executions.list(currentPage, 10)
      .then((data) => { if (!cancelled) setPage(data) })
      .catch((e) => { if (!cancelled) setError(e.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [currentPage])

  return (
    <div className="audit-log">
      <div className="list-header">
        <h1 className="page-title">Audit log</h1>
        <Link to="/" className="btn btn-secondary">Workflows</Link>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <div className="loading">Loading…</div>}

      {!loading && !error && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Execution ID</th>
                <th>Workflow</th>
                <th>Version</th>
                <th>Status</th>
                <th>Started by</th>
                <th>Start time</th>
                <th>End time</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {page.content?.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty-state">No executions yet.</td>
                </tr>
              ) : (
                page.content?.map((e) => (
                  <tr key={e.id}>
                    <td className="id-cell">{String(e.id).slice(0, 8)}…</td>
                    <td>{e.workflowName ?? (e.workflowId ? String(e.workflowId).slice(0, 8) + '…' : '—')}</td>
                    <td>{e.workflowVersion ?? '—'}</td>
                    <td>
                      <span className={`badge badge-${(e.status || '').toLowerCase().replace(' ', '_')}`}>
                        {e.status}
                      </span>
                    </td>
                    <td>{e.triggeredBy ? String(e.triggeredBy).slice(0, 8) + '…' : '—'}</td>
                    <td>{e.startedAt ? new Date(e.startedAt).toISOString() : '—'}</td>
                    <td>{e.endedAt ? new Date(e.endedAt).toISOString() : '—'}</td>
                    <td>
                      <Link to={`/executions/${e.id}`} className="btn btn-secondary btn-sm">
                        View logs
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {page.totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => p - 1)}
              >
                Previous
              </button>
              <span>Page {currentPage + 1} of {page.totalPages}</span>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={currentPage >= page.totalPages - 1}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
