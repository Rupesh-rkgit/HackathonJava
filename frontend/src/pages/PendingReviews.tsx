import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Mcq, Page } from '../api/types'
import { Modal } from '../components/Modal'
import { Pagination } from '../components/Pagination'
import { useToast } from '../components/Toast'
import './review.css'

export default function PendingReviews() {
  const [data, setData] = useState<Page<Mcq> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [viewing, setViewing] = useState<Mcq | null>(null)
  const [comments, setComments] = useState('')
  const [busy, setBusy] = useState(false)
  const [rejectMode, setRejectMode] = useState(false)
  const toast = useToast()

  async function load(p = page) {
    setLoading(true)
    try { setData(await api.pendingReviews(p)) }
    catch (e) { toast('error', e instanceof ApiError ? e.message : 'Failed to load') }
    finally { setLoading(false) }
  }
  useEffect(() => { load(page) /* eslint-disable-next-line */ }, [page])

  function open(m: Mcq) { setViewing(m); setComments(''); setRejectMode(false) }

  async function approve() {
    if (!viewing) return
    setBusy(true)
    try {
      await api.approve(viewing.id)
      toast('success', `Question #${viewing.id} approved.`)
      setViewing(null); load()
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Approve failed') }
    finally { setBusy(false) }
  }

  async function reject() {
    if (!viewing) return
    if (!comments.trim()) { toast('error', 'A comment is required to reject.'); return }
    setBusy(true)
    try {
      await api.reject(viewing.id, comments.trim())
      toast('success', `Question #${viewing.id} returned with feedback.`)
      setViewing(null); load()
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Reject failed') }
    finally { setBusy(false) }
  }

  const items = data?.content ?? []
  const optionLetters: Array<'A' | 'B' | 'C' | 'D'> = ['A', 'B', 'C', 'D']

  return (
    <div>
      <div className="page-head">
        <span className="page-eyebrow">Reviewer Desk</span>
        <h1 className="page-title">My Pending <em>Reviews</em></h1>
        <p className="page-sub">Questions assigned to you for evaluation. Approve when they meet the bar, or return them with actionable feedback.</p>
      </div>

      <div className="card section-card">
        {loading ? (
          <div style={{ padding: 16 }}>{[0, 1, 2].map((i) => <div key={i} className="skeleton-row" />)}</div>
        ) : items.length === 0 ? (
          <div className="state-box">
            <div className="se-glyph">◑</div>
            <h3>Nothing awaiting your review</h3>
            <p>When an admin assigns you a question, it will appear here with a pending label.</p>
          </div>
        ) : (
          <table className="qtable">
            <thead>
              <tr>
                <th style={{ width: 56 }}>ID</th>
                <th>Question</th>
                <th style={{ width: 150 }}>Stack</th>
                <th style={{ width: 110 }}>Label</th>
                <th style={{ width: 150, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((m) => (
                <tr key={m.id}>
                  <td className="q-id">#{m.id}</td>
                  <td className="q-stem"><strong>{m.questionStem}</strong><div className="q-meta">{m.topicName}</div></td>
                  <td>{m.stackName}</td>
                  <td><span className="pill UNDER_REVIEW"><span className="dot" />Pending</span></td>
                  <td><div className="q-actions"><button className="btn btn-primary btn-sm" onClick={() => open(m)}>View &amp; Review</button></div></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}
      </div>

      <Modal
        open={!!viewing}
        onClose={() => setViewing(null)}
        title={`Review Question #${viewing?.id ?? ''}`}
        subtitle={viewing ? `${viewing.stackName} · ${viewing.topicName} · ${viewing.difficulty}` : ''}
        width={720}
      >
        {viewing && (
          <div className="rv">
            <p className="rv-stem">{viewing.questionStem}</p>
            <div className="rv-options">
              {optionLetters.map((L) => {
                const text = viewing[`option${L}` as 'optionA']
                const correct = viewing.correctAnswer === L
                return (
                  <div key={L} className={`rv-opt ${correct ? 'correct' : ''}`}>
                    <span className="rv-letter">{L}</span>
                    <span>{text}</span>
                    {correct && <span className="rv-correct-flag">✓ Correct answer</span>}
                  </div>
                )
              })}
            </div>

            {rejectMode && (
              <div className="rv-reject">
                <label className="field-label">Rejection feedback (required)</label>
                <textarea
                  className="textarea" autoFocus value={comments}
                  placeholder="Explain what needs to change so the author can revise…"
                  onChange={(e) => setComments(e.target.value)}
                />
              </div>
            )}

            <div className="rv-actions">
              <div className="spacer" />
              {!rejectMode ? (
                <>
                  <button className="btn btn-danger" disabled={busy} onClick={() => setRejectMode(true)}>Reject…</button>
                  <button className="btn btn-accent" disabled={busy} onClick={approve}>✓ Approve</button>
                </>
              ) : (
                <>
                  <button className="btn btn-ghost" disabled={busy} onClick={() => setRejectMode(false)}>Back</button>
                  <button className="btn btn-danger" disabled={busy || !comments.trim()} onClick={reject}>Confirm Rejection</button>
                </>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
