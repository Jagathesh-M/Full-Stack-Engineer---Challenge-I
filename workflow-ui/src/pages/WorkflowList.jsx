import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import './WorkflowList.css'

export default function WorkflowList() {
  const [workflows, setWorkflows] = useState({ content: [], totalPages: 0 })
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    api.workflows.list(search, page, 10)
      .then((data) => {
        if (!cancelled) setWorkflows(data)
      })
      .catch((e) => { if (!cancelled) setError(e.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [search, page])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
  }

  return (
    <div className="workflow-list">
      <div className="list-header">
        <h1 className="page-title">Workflows</h1>
        <Link to="/workflows/new" className="btn btn-primary">Create workflow</Link>
      </div>

      <form onSubmit={handleSearch} className="toolbar">
        <input
          type="text"
          placeholder="Search by name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="search-input"
        />
        <button type="submit" className="btn btn-secondary">Search</button>
      </form>

      {error && <div className="error-banner">{error}</div>}
      {loading && <div className="loading">Loading…</div>}

      {!loading && !error && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Steps</th>
                <th>Version</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {workflows.content?.length === 0 ? (
                <tr>
                  <td colSpan={6} className="empty-state">No workflows yet. Create one to get started.</td>
                </tr>
              ) : (
                workflows.content?.map((w) => (
                  <tr key={w.id}>
                    <td className="id-cell">{String(w.id).slice(0, 8)}…</td>
                    <td>{w.name}</td>
                    <td>{w.stepCount ?? '—'}</td>
                    <td>{w.version}</td>
                    <td>
                      <span className={`badge ${w.isActive ? 'badge-active' : 'badge-inactive'}`}>
                        {w.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div className="action-buttons">
                        <Link to={`/workflows/${w.id}/edit`} className="btn btn-secondary btn-sm">Edit</Link>
                        <Link to={`/workflows/${w.id}/execute`} className="btn btn-primary btn-sm">Execute</Link>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {workflows.totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </button>
              <span>Page {page + 1} of {workflows.totalPages}</span>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page >= workflows.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
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
