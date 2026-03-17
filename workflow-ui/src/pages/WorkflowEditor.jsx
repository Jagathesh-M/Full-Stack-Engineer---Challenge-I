import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import './WorkflowEditor.css'

const STEP_TYPES = ['task', 'approval', 'notification']

export default function WorkflowEditor() {
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const [workflow, setWorkflow] = useState(null)
  const [detail, setDetail] = useState(null)
  const [name, setName] = useState('')
  const [inputSchema, setInputSchema] = useState({})
  const [schemaJson, setSchemaJson] = useState('{}')
  const [steps, setSteps] = useState([])
  const [loading, setLoading] = useState(!isNew)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [schemaPreview, setSchemaPreview] = useState([])

  useEffect(() => {
    if (isNew) {
      setWorkflow({})
      setSteps([])
      setLoading(false)
      return
    }
    api.workflows.getDetail(id)
      .then((d) => {
        setDetail(d)
        setWorkflow(d.workflow)
        setName(d.workflow?.name ?? '')
        setInputSchema(d.workflow?.inputSchema ?? {})
        setSchemaJson(JSON.stringify(d.workflow?.inputSchema ?? {}, null, 2))
        setSteps(d.steps ?? [])
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id, isNew])

  useEffect(() => {
    try {
      const parsed = JSON.parse(schemaJson || '{}')
      const keys = Object.keys(parsed || {})
      const preview = keys.map((k) => {
        const def = parsed[k]
        if (def && typeof def === 'object') {
          const allowed = Array.isArray(def.allowed_values) ? def.allowed_values : null
          return {
            key: k,
            type: def.type ?? 'string',
            required: !!def.required,
            allowed,
          }
        }
        return { key: k, type: 'string', required: false, allowed: null }
      })
      setSchemaPreview(preview)
    } catch {
      setSchemaPreview([])
    }
  }, [schemaJson])

  const handleSaveWorkflow = (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    let parsed = {}
    try {
      parsed = JSON.parse(schemaJson)
    } catch {
      setError('Invalid JSON for input schema')
      setSaving(false)
      return
    }
    const payload = { name, inputSchema: parsed, isActive: true }
    if (isNew) {
      api.workflows.create(payload)
        .then((w) => {
          setWorkflow(w)
          navigate(`/workflows/${w.id}/edit`, { replace: true })
        })
        .catch((e) => setError(e.message))
        .finally(() => setSaving(false))
    } else {
      api.workflows.update(id, { ...payload, startStepId: workflow?.startStepId })
        .then((w) => {
          setWorkflow(w)
          setDetail((d) => d ? { ...d, workflow: w } : null)
        })
        .catch((e) => setError(e.message))
        .finally(() => setSaving(false))
    }
  }

  const handleAddStep = () => {
    const workflowId = id || workflow?.id
    if (!workflowId) {
      setError('Save the workflow first before adding steps.')
      return
    }
    const name = prompt('Step name')
    if (!name) return
    const stepType = prompt('Step type: task, approval, or notification', 'task')
    if (!STEP_TYPES.includes(stepType)) {
      setError('Invalid step type')
      return
    }
    api.steps.add(workflowId, {
      name,
      stepType,
      order: steps.length,
    })
      .then((step) => setSteps((s) => [...s, step]))
      .catch((e) => setError(e.message))
  }

  const handleEditStep = (step) => {
    const newName = prompt('Step name', step.name)
    if (!newName) return
    const newType = prompt('Step type: task, approval, or notification', step.stepType)
    if (!STEP_TYPES.includes(newType)) {
      setError('Invalid step type')
      return
    }
    api.steps.update(step.id, { name: newName, stepType: newType })
      .then((updated) => setSteps((prev) => prev.map((s) => (s.id === updated.id ? updated : s))))
      .catch((e) => setError(e.message))
  }

  const handleSetStartStep = (stepId) => {
    if (!id) return
    setSaving(true)
    setError(null)
    api.workflows.update(id, { name, inputSchema: inputSchema ?? {}, isActive: true, startStepId: stepId })
      .then((w) => {
        setWorkflow(w)
        setDetail((d) => d ? { ...d, workflow: w } : null)
      })
      .catch((e) => setError(e.message))
      .finally(() => setSaving(false))
  }

  const handleDeleteStep = (stepId) => {
    if (!confirm('Delete this step?')) return
    api.steps.delete(stepId)
      .then(() => setSteps((s) => s.filter((x) => x.id !== stepId)))
      .catch((e) => setError(e.message))
  }

  if (loading) return <div className="loading">Loading…</div>
  if (!isNew && !workflow) return <div className="error-banner">Workflow not found</div>

  return (
    <div className="workflow-editor">
      <div className="editor-header">
        <h1 className="page-title">
          {isNew ? 'New workflow' : `Edit: ${workflow?.name ?? name} (v${workflow?.version ?? 1})`}
        </h1>
        {!isNew && (
          <Link to="/" className="btn btn-secondary">Back to list</Link>
        )}
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form onSubmit={handleSaveWorkflow} className="card section">
        <h2>Workflow</h2>
        <div className="form-group">
          <label>Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        {!isNew && (
          <div className="form-group">
            <label>Start step</label>
            <select
              value={workflow?.startStepId ?? ''}
              onChange={(e) => handleSetStartStep(e.target.value || null)}
              disabled={saving}
            >
              <option value="">(not set)</option>
              {steps.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
        )}
        <div className="form-group">
          <label>Input schema (JSON)</label>
          <textarea
            value={schemaJson}
            onChange={(e) => setSchemaJson(e.target.value)}
            rows={10}
            className="schema-textarea"
          />
        </div>
        {schemaPreview.length > 0 && (
          <div className="schema-preview">
            <div className="schema-preview-title">Preview</div>
            <ul className="schema-preview-list">
              {schemaPreview.map((f) => (
                <li key={f.key}>
                  <code>{f.key}</code>
                  <span className="schema-chip">{f.type}</span>
                  {f.required && <span className="schema-chip schema-chip-required">required</span>}
                  {f.allowed && (
                    <span className="schema-allowed">
                      ({f.allowed.join(' | ')})
                    </span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {isNew ? 'Create workflow' : 'Save workflow'}
        </button>
      </form>

      {!isNew && (
        <div className="card section">
          <h2>Steps</h2>
          <ul className="steps-list">
            {steps.map((s) => (
              <li key={s.id} className="step-item">
                <span className="step-order">{s.order + 1}.</span>
                <span className="step-name">{s.name}</span>
                <span className="badge step-type">{s.stepType}</span>
                {workflow?.startStepId === s.id && <span className="badge badge-start">Start</span>}
                <div className="action-buttons">
                  <Link to={`/workflows/${id}/steps/${s.id}/rules`} className="btn btn-secondary btn-sm">
                    Rules
                  </Link>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleEditStep(s)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={() => handleDeleteStep(s.id)}
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <button type="button" className="btn btn-secondary" onClick={handleAddStep}>
            Add step
          </button>
        </div>
      )}
    </div>
  )
}
