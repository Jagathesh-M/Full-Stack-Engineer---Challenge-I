import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api/client'
import './StepRuleEditor.css'

export default function StepRuleEditor() {
  const { id: workflowId, stepId } = useParams()
  const [step, setStep] = useState(null)
  const [rules, setRules] = useState([])
  const [steps, setSteps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [editingRule, setEditingRule] = useState(null)
  const [condition, setCondition] = useState('')
  const [nextStepId, setNextStepId] = useState('')
  const [priority, setPriority] = useState(1)

  useEffect(() => {
    Promise.all([
      api.workflows.getDetail(workflowId),
      api.rules.list(stepId),
    ])
      .then(([detail, rulesList]) => {
        const s = (detail.steps || []).find((x) => x.id === stepId)
        setStep(s)
        setSteps(detail.steps || [])
        setRules(rulesList)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [workflowId, stepId])

  const handleSaveRule = (e) => {
    e.preventDefault()
    if (editingRule) {
      api.rules.update(editingRule.id, { condition, nextStepId: nextStepId || null, priority })
        .then((r) => {
          setRules((prev) => prev.map((x) => (x.id === r.id ? r : x)))
          setEditingRule(null)
          resetForm()
        })
        .catch((e) => setError(e.message))
    } else {
      api.rules.add(stepId, { condition, nextStepId: nextStepId || null, priority })
        .then((r) => {
          setRules((prev) => [...prev, r].sort((a, b) => a.priority - b.priority))
          resetForm()
        })
        .catch((e) => setError(e.message))
    }
  }

  const resetForm = () => {
    setCondition('DEFAULT')
    setNextStepId('')
    setPriority(rules.length + 1)
  }

  const startAdd = () => {
    setEditingRule(null)
    setCondition('DEFAULT')
    setNextStepId('')
    setPriority(rules.length + 1)
  }

  const startEdit = (rule) => {
    setEditingRule(rule)
    setCondition(rule.condition)
    setNextStepId(rule.nextStepId ?? '')
    setPriority(rule.priority)
  }

  const handleDeleteRule = (ruleId) => {
    if (!confirm('Delete this rule?')) return
    api.rules.delete(ruleId)
      .then(() => setRules((prev) => prev.filter((r) => r.id !== ruleId)))
      .catch((e) => setError(e.message))
  }

  const stepName = (sid) => steps.find((s) => s.id === sid)?.name ?? (sid || 'End')

  if (loading) return <div className="loading">Loading…</div>

  return (
    <div className="step-rule-editor">
      <div className="editor-header">
        <h1 className="page-title">Rules: {step?.name ?? stepId}</h1>
        <Link to={`/workflows/${workflowId}/edit`} className="btn btn-secondary">Back to workflow</Link>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card section">
        <h2>Rules (evaluated by priority, first match wins)</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Priority</th>
                <th>Condition</th>
                <th>Next step</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((r) => (
                <tr key={r.id}>
                  <td>{r.priority}</td>
                  <td className="condition-cell"><code>{r.condition}</code></td>
                  <td>{stepName(r.nextStepId)}</td>
                  <td>
                    <div className="action-buttons">
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => startEdit(r)}>Edit</button>
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDeleteRule(r.id)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <form onSubmit={handleSaveRule} className="rule-form">
          <h3>{editingRule ? 'Edit rule' : 'Add rule'}</h3>
          <div className="form-row">
            <div className="form-group">
              <label>Priority</label>
              <input
                type="number"
                value={priority}
                onChange={(e) => setPriority(Number(e.target.value))}
                min={1}
              />
            </div>
            <div className="form-group flex-grow">
              <label>Condition</label>
              <input
                value={condition}
                onChange={(e) => setCondition(e.target.value)}
                placeholder="e.g. amount > 100 && country == 'US' or DEFAULT"
              />
            </div>
            <div className="form-group">
              <label>Next step</label>
              <select
                value={nextStepId}
                onChange={(e) => setNextStepId(e.target.value)}
              >
                <option value="">End workflow</option>
                {steps.filter((s) => s.id !== stepId).map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary">
              {editingRule ? 'Update' : 'Add'}
            </button>
            {editingRule && (
              <button type="button" className="btn btn-secondary" onClick={startAdd}>
                Cancel
              </button>
            )}
          </div>
        </form>
        {!editingRule && (
          <button type="button" className="btn btn-secondary" onClick={startAdd}>
            Add new rule
          </button>
        )}
      </div>
    </div>
  )
}
