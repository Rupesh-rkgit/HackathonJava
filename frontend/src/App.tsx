import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import AppShell from './components/AppShell'
import Login from './pages/Login'
import MyQuestions from './pages/MyQuestions'
import AddQuestion from './pages/AddQuestion'
import PendingReviews from './pages/PendingReviews'
import QuestionBank from './pages/QuestionBank'
import type { ReactNode } from 'react'

function Protected({ children, adminOnly }: { children: ReactNode; adminOnly?: boolean }) {
  const { user, isAdmin, ready } = useAuth()
  if (!ready) return null
  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/questions" replace />
  return <>{children}</>
}

export default function App() {
  const { user, ready } = useAuth()
  if (!ready) return null

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/questions" replace /> : <Login />} />
      <Route element={<Protected><AppShell /></Protected>}>
        <Route path="/questions" element={<MyQuestions />} />
        <Route path="/add" element={<AddQuestion />} />
        <Route path="/reviews" element={<PendingReviews />} />
        <Route path="/bank" element={<Protected adminOnly><QuestionBank /></Protected>} />
      </Route>
      <Route path="*" element={<Navigate to={user ? '/questions' : '/login'} replace />} />
    </Routes>
  )
}
