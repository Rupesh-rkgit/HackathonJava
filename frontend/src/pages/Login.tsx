import { motion } from 'framer-motion'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import './login.css'

const DEMO = [
  { id: 'birendra.kumar.singh', label: 'Birendra', role: 'Admin' },
  { id: 'gaurav.a.bhola', label: 'Gaurav', role: 'SME' },
  { id: 'divya.madhanasekar', label: 'Divya', role: 'SME' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [enterpriseId, setEnterpriseId] = useState('')
  const [password, setPassword] = useState('password')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(enterpriseId.trim(), password)
      navigate('/questions')
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401
        ? 'Invalid enterprise ID or password.'
        : 'Could not sign in. Is the server running?')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-shell">
      {/* — brand / editorial panel — */}
      <aside className="login-brand">
        <div className="lb-grain" />
        <motion.div
          className="lb-content"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.2, 0.8, 0.2, 1] }}
        >
          <span className="lb-eyebrow">ATCI · Learning &amp; Talent Transformation</span>
          <h1 className="lb-title">
            Smart&nbsp;Quiz<br /><em>AI&nbsp;Hub</em>
          </h1>
          <p className="lb-lede">
            A centralized workshop where subject-matter experts author,
            review, and approve assessment questions — every item passing
            through a single, accountable pipeline.
          </p>

          <div className="lb-rail">
            {['Draft', 'Ready', 'Review', 'Approved'].map((s, i) => (
              <motion.div
                key={s}
                className="lb-rail-step"
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.4 + i * 0.12 }}
              >
                <span className="lb-rail-dot" />
                <span>{s}</span>
                {i < 3 && <span className="lb-rail-line" />}
              </motion.div>
            ))}
          </div>
        </motion.div>
        <footer className="lb-foot">Hack-N-Stack · Code the Future · 2026</footer>
      </aside>

      {/* — form panel — */}
      <main className="login-main">
        <motion.form
          className="login-form"
          onSubmit={submit}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.15 }}
        >
          <h2 className="lf-h">Sign in</h2>
          <p className="lf-sub">Use your enterprise ID to enter the hub.</p>

          <label className="field-label" htmlFor="eid">Enterprise ID</label>
          <input
            id="eid" className="input" autoFocus autoComplete="username"
            placeholder="firstname.lastname"
            value={enterpriseId}
            onChange={(e) => setEnterpriseId(e.target.value)}
          />

          <label className="field-label" htmlFor="pwd" style={{ marginTop: 16 }}>Password</label>
          <input
            id="pwd" className="input" type="password" autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && <div className="lf-error">{error}</div>}

          <button className="btn btn-primary lf-submit" disabled={busy || !enterpriseId}>
            {busy ? 'Signing in…' : 'Enter the Hub →'}
          </button>

          <div className="lf-demo">
            <span className="lf-demo-label">Quick demo logins</span>
            <div className="lf-demo-chips">
              {DEMO.map((d) => (
                <button
                  type="button" key={d.id} className="lf-chip"
                  onClick={() => { setEnterpriseId(d.id); setPassword('password') }}
                >
                  <strong>{d.label}</strong>
                  <span>{d.role}</span>
                </button>
              ))}
            </div>
            <p className="lf-demo-hint">All demo accounts use password <code>password</code>.</p>
          </div>
        </motion.form>
      </main>
    </div>
  )
}
