import { motion } from 'framer-motion'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './appshell.css'

interface NavItem { to: string; label: string; icon: string; adminOnly?: boolean }

const NAV: NavItem[] = [
  { to: '/questions', label: 'My Questions', icon: '◳' },
  { to: '/add', label: 'Add Question', icon: '＋' },
  { to: '/reviews', label: 'My Pending Reviews', icon: '◑' },
  { to: '/bank', label: 'Question Bank', icon: '▤', adminOnly: true },
]

function initials(name: string) {
  return name.split(/[ .]/).filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase()).join('')
}

export default function AppShell() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  function doLogout() { logout(); navigate('/login') }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sb-brand">
          <span className="sb-mark">Q</span>
          <div className="sb-brand-text">
            <strong>Smart Quiz</strong>
            <span>AI Hub</span>
          </div>
        </div>

        <nav className="sb-nav">
          {NAV.filter((n) => !n.adminOnly || isAdmin).map((n) => (
            <NavLink
              key={n.to} to={n.to}
              className={({ isActive }) => `sb-link ${isActive ? 'active' : ''}`}
            >
              <span className="sb-ico">{n.icon}</span>
              {n.label}
            </NavLink>
          ))}
        </nav>

        <div className="sb-foot">
          <div className="sb-user">
            <span className="sb-avatar">{initials(user.name)}</span>
            <div className="sb-user-text">
              <strong>{user.name}</strong>
              <span className={`sb-role ${user.role}`}>{user.role}</span>
            </div>
          </div>
          <button className="sb-logout" onClick={doLogout}>Sign out</button>
        </div>
      </aside>

      <div className="main-col">
        <motion.main
          className="content"
          key={location.pathname}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, ease: [0.2, 0.8, 0.2, 1] }}
        >
          <Outlet />
        </motion.main>
      </div>
    </div>
  )
}
