import { Routes, Route, NavLink } from 'react-router-dom'
import WorkflowList from './pages/WorkflowList'
import WorkflowEditor from './pages/WorkflowEditor'
import StepRuleEditor from './pages/StepRuleEditor'
import WorkflowExecute from './pages/WorkflowExecute'
import ExecutionDetail from './pages/ExecutionDetail'
import AuditLog from './pages/AuditLog'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Workflow Engine</h1>
        <nav>
          <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>Workflows</NavLink>
          <NavLink to="/audit" className={({ isActive }) => isActive ? 'active' : ''}>Audit Log</NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<WorkflowList />} />
          <Route path="/workflows/new" element={<WorkflowEditor />} />
          <Route path="/workflows/:id/edit" element={<WorkflowEditor />} />
          <Route path="/workflows/:id/steps/:stepId/rules" element={<StepRuleEditor />} />
          <Route path="/workflows/:id/execute" element={<WorkflowExecute />} />
          <Route path="/executions/:id" element={<ExecutionDetail />} />
          <Route path="/audit" element={<AuditLog />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
