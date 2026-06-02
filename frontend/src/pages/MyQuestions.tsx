import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { Mcq, McqRequest, Page, SaveMode } from '../api/types'
import { Modal } from '../components/Modal'
import { McqForm } from '../components/McqForm'
import { Pagination } from '../components/Pagination'
import { StatusPill } from '../components/StatusPill'
import { LifecyclePipeline } from '../components/LifecyclePipeline'
import { useToast } from '../components/Toast'

export default function MyQuestions() {
  const [data, setData] = useState<Page<Mcq> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Mcq | null>(null)
  const [saving, setSaving] = useState(false)
  const toast = useToast()
  const navigate = useNavigate()

  async function load(p = page) {
    setLoading(true)
    try { setData(await api.myQuestions(p)) }
    catch (e) { toast('error', e instanceof ApiError ? e.message : 'Failed to load') }
    finally { setLoading(false) }
  }
  useEffect(() => { load(page) /* eslint-disable-next-line */ }, [page])

  const editable = (m: Mcq) => m.status === 'DRAFT' || m.status === 'REJECTED'

  async function openEdit(m: Mcq) {
    try { setEditing(await api.getMcq(m.id)) }
    catch { setEditing(m) }
  }

  async function submitEdit(body: McqRequest, mode: SaveMode) {
    if (!editing) return
    setSaving(true)
    try {
      await api.updateMcq(editing.id, body)
      toast('success', mode === 'SAVE_AND_SEND' ? 'Saved and sent for review.' : 'Question saved.')
      setEditing(null)
      load()
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Save failed')
    } finally { setSaving(false) }
  }

  const items = data?.content ?? []

  return (
    <div>
      <div className="page-head">
        <span className="page-eyebrow">My Workspace</span>
        <h1 className="page-title">My <em>Questions</em></h1>
        <p className="page-sub">
          Every MCQ you've authored and its place in the review pipeline.
          Draft and rejected items can be edited and resubmitted.
        </p>
      </div>

      <div className="card section-card">
        {loading ? (
          <div style={{ padding: 16 }}>{[0, 1, 2, 3].map((i) => <div key={i} className="skeleton-row" />)}</div>
        ) : items.length === 0 ? (
          <div className="state-box">
            <div className="se-glyph">∅</div>
            <h3>No questions yet</h3>
            <p>Start by adding a question from the UI or uploading a batch via Excel.</p>
            <button className="btn btn-accent" style={{ marginTop: 18 }} onClick={() => navigate('/add')}>Add your first question</button>
          </div>
        ) : (
          <table className="qtable">
            <thead>
              <tr>
                <th style={{ width: 56 }}>ID</th>
                <th>Question</th>
                <th style={{ width: 150 }}>Stack</th>
                <th style={{ width: 96 }}>Difficulty</th>
                <th style={{ width: 168 }}>Status</th>
                <th style={{ width: 120, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((m) => (
                <tr key={m.id}>
                  <td className="q-id">#{m.id}</td>
                  <td className="q-stem">
                    <strong>{m.questionStem}</strong>
                    <div className="q-meta">{m.topicName}</div>
                    {m.status === 'REJECTED' && m.reviewerComments && (
                      <div className="review-comment">
                        <strong>Reviewer feedback</strong>
                        {m.reviewerComments}
                      </div>
                    )}
                  </td>
                  <td>{m.stackName}</td>
                  <td><span className={`chip-diff ${m.difficulty}`}>{m.difficulty}</span></td>
                  <td><StatusPill status={m.status} /></td>
                  <td>
                    <div className="q-actions">
                      {editable(m)
                        ? <button className="btn btn-ghost btn-sm" onClick={() => openEdit(m)}>Edit</button>
                        : <span style={{ fontSize: 12.5, color: 'var(--ink-faint)' }}>—</span>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}
      </div>

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title={`Edit Question #${editing?.id ?? ''}`}
        subtitle={
          editing && (
            <div style={{ marginTop: 12 }}>
              <LifecyclePipeline status={editing.status} />
            </div>
          )
        }
        width={720}
      >
        {editing && (
          <>
            {editing.status === 'REJECTED' && editing.reviewerComments && (
              <div className="review-comment" style={{ marginBottom: 18 }}>
                <strong>Reviewer feedback to address</strong>
                {editing.reviewerComments}
              </div>
            )}
            <McqForm initial={editing} submitting={saving} onSubmit={submitEdit} onCancel={() => setEditing(null)} />
          </>
        )}
      </Modal>
    </div>
  )
}
