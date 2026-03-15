import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import './WorkflowExecute.css'

export default function WorkflowExecute() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState(null)
  const [inputData, setInputData] = useState({})
  const [loading, setLoading] = useState(true)
  const [executing, setExecuting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.workflows.getDetail(id)
      .then(setDetail)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!detail?.workflow?.inputSchema) return
    const schema = detail.workflow.inputSchema
    const initial = {}
    Object.keys(schema).forEach((key) => {
      const def = schema[key]
      if (def && typeof def === 'object') {
        if (def.type === 'number') initial[key] = 0
        else if (def.allowed_values && Array.isArray(def.allowed_values)) initial[key] = def.allowed_values[0] ?? ''
        else initial[key] = ''
      }
    })
    setInputData((prev) => ({ ...initial, ...prev }))
  }, [detail?.workflow?.inputSchema])

  const handleChange = (key, value) => {
    const schema = detail?.workflow?.inputSchema?.[key]
    if (schema?.type === 'number') value = Number(value) || 0
    setInputData((prev) => ({ ...prev, [key]: value }))
  }

  const handleStart = (e) => {
    e.preventDefault()
    setExecuting(true)
    setError(null)
    api.executions.start(id, { data: inputData })
      .then((exec) => navigate(`/executions/${exec.id}`))
      .catch((e) => setError(e.message))
      .finally(() => setExecuting(false))
  }

  if (loading) return <div className="loading">Loading…</div>
  if (!detail) return <div className="error-banner">Workflow not found</div>

  const schema = detail.workflow?.inputSchema ?? {}
  const keys = Object.keys(schema)

  return (
    <div className="workflow-execute">
      <div className="editor-header">
        <h1 className="page-title">Execute: {detail.workflow?.name}</h1>
        <Link to="/" className="btn btn-secondary">Back to list</Link>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card section">
        <h2>Input data</h2>
        <form onSubmit={handleStart}>
          {keys.length === 0 ? (
            <p className="text-muted">No input schema defined. You can still run with empty data.</p>
          ) : (
            <div className="input-fields">
              {keys.map((key) => {
                const def = schema[key]
                const isNum = def?.type === 'number'
                const allowed = def?.allowed_values
                return (
                  <div key={key} className="form-group">
                    <label>
                      {key}
                      {def?.required && <span className="required">*</span>}
                      {def?.type && <span className="type-hint"> ({def.type})</span>}
                    </label>
                    {allowed && Array.isArray(allowed) ? (
                      <select
                        value={inputData[key] ?? ''}
                        onChange={(e) => handleChange(key, e.target.value)}
                      >
                        {allowed.map((v) => (
                          <option key={v} value={v}>{v}</option>
                        ))}
                      </select>
                    ) : (
                      <input
                        type={isNum ? 'number' : 'text'}
                        value={inputData[key] ?? ''}
                        onChange={(e) => handleChange(key, e.target.value)}
                        required={!!def?.required}
                      />
                    )}
                  </div>
                )
              })}
            </div>
          )}
          <button type="submit" className="btn btn-primary" disabled={executing}>
            Start execution
          </button>
        </form>
      </div>
    </div>
  )
}
