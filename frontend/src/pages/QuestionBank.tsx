import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { EligibleReviewer, Mcq, McqRequest, Page, SaveMode } from '../api/types'
import { Modal } from '../components/Modal'
import { McqForm } from '../components/McqForm'
import { Pagination } from '../components/Pagination'
import { StatusPill } from '../components/StatusPill'
import { BulkBar } from '../components/BulkBar'
import { useToast } from '../components/Toast'

export default function QuestionBank() {
  const [data, setData] = useState<Page<Mcq> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Mcq | null>(null)
  const [assigning, setAssigning] = useState<Mcq | null>(null)
  const [bulkOpen, setBulkOpen] = useState(false)
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  async function load(p = page) {
    setLoading(true)
    try {
      setData(await api.allMcqs(p))
      setSelected(new Set())
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Failed to load') }
    finally { setLoading(false) }
  }
  useEffect(() => { load(page) /* eslint-disable-next-line */ }, [page])

  async function openEdit(m: Mcq) {
    try { setEditing(await api.getMcq(m.id)) } catch { setEditing(m) }
  }

  async function submitEdit(body: McqRequest, _mode: SaveMode) {
    if (!editing) return
    setSaving(true)
    try {
      await api.adminUpdateMcq(editing.id, body)
      toast('success', `Question #${editing.id} updated.`)
      setEditing(null); load()
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Save failed') }
    finally { setSaving(false) }
  }

  const items = data?.content ?? []
  const assignable = (m: Mcq) => m.status === 'READY_FOR_REVIEW'
  const selectableIds = items.filter(assignable).map((m) => m.id)
  const allSelected = selectableIds.length > 0 && selectableIds.every((id) => selected.has(id))

  function toggle(id: number) {
    setSelected((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }
  function toggleAll() {
    setSelected(allSelected ? new Set() : new Set(selectableIds))
  }

  return (
    <div>
      <div className="page-head">
        <span className="page-eyebrow">Administration</span>
        <h1 className="page-title">Question <em>Bank</em></h1>
        <p className="page-sub">Every MCQ across all authors. Assign skill-matched reviewers to ready items — one at a time or in bulk — and edit any question regardless of its state.</p>
      </div>

      <div className="card section-card">
        {loading ? (
          <div style={{ padding: 16 }}>{[0, 1, 2, 3, 4].map((i) => <div key={i} className="skeleton-row" />)}</div>
        ) : items.length === 0 ? (
          <div className="state-box"><div className="se-glyph">▤</div><h3>The bank is empty</h3><p>Once SMEs author questions they'll appear here.</p></div>
        ) : (
          <table className="qtable">
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox" className="row-check" aria-label="Select all assignable"
                    checked={allSelected} disabled={selectableIds.length === 0}
                    onChange={toggleAll}
                  />
                </th>
                <th style={{ width: 48 }}>ID</th>
                <th>Question</th>
                <th style={{ width: 130 }}>Creator</th>
                <th style={{ width: 150 }}>Status</th>
                <th style={{ width: 190, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((m) => (
                <tr key={m.id} className={selected.has(m.id) ? 'row-selected' : ''}>
                  <td>
                    {assignable(m) ? (
                      <input
                        type="checkbox" className="row-check"
                        aria-label={`Select question ${m.id}`}
                        checked={selected.has(m.id)} onChange={() => toggle(m.id)}
                      />
                    ) : null}
                  </td>
                  <td className="q-id">#{m.id}</td>
                  <td className="q-stem"><strong>{m.questionStem}</strong><div className="q-meta">{m.stackName} · {m.topicName}</div></td>
                  <td className="q-eid">{m.creatorEnterpriseId}</td>
                  <td><StatusPill status={m.status} /></td>
                  <td>
                    <div className="q-actions">
                      {assignable(m) && (
                        <button className="btn btn-accent btn-sm" onClick={() => setAssigning(m)}>Assign Reviewer</button>
                      )}
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(m)}>Edit</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}
      </div>

      <BulkBar
        count={selected.size}
        onClear={() => setSelected(new Set())}
        actionLabel="Assign Reviewer →"
        onAction={() => setBulkOpen(true)}
        noun="question"
      />

      <Modal open={!!editing} onClose={() => setEditing(null)} title={`Edit Question #${editing?.id ?? ''}`}
        subtitle="Admin super-edit — saves changes without altering the review status." width={720}>
        {editing && <McqForm initial={editing} allowSend={false} submitting={saving} onSubmit={submitEdit} onCancel={() => setEditing(null)} />}
      </Modal>

      {assigning && (
        <AssignDialog mcq={assigning} onClose={() => setAssigning(null)} onAssigned={() => { setAssigning(null); load() }} />
      )}

      {bulkOpen && (
        <BulkAssignDialog
          ids={[...selected]}
          onClose={() => setBulkOpen(false)}
          onAssigned={() => { setBulkOpen(false); load() }}
        />
      )}
    </div>
  )
}

function AssignDialog({ mcq, onClose, onAssigned }: { mcq: Mcq; onClose: () => void; onAssigned: () => void }) {
  const [reviewers, setReviewers] = useState<EligibleReviewer[] | null>(null)
  const [selected, setSelected] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  useEffect(() => {
    api.eligibleReviewers(mcq.id).then(setReviewers).catch((e) => {
      toast('error', e instanceof ApiError ? e.message : 'Could not load reviewers'); setReviewers([])
    })
  }, [mcq.id])

  async function assign() {
    if (!selected) return
    setBusy(true)
    try {
      await api.assignReviewer(mcq.id, selected)
      toast('success', `Assigned to ${selected}. Question is now under review.`)
      onAssigned()
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Assignment failed') }
    finally { setBusy(false) }
  }

  return (
    <Modal open onClose={onClose} title="Assign Reviewer"
      subtitle={`#${mcq.id} · ${mcq.stackName} · by ${mcq.creatorEnterpriseId}`} width={540}>
      <p style={{ color: 'var(--ink-soft)', fontSize: 14.5, marginBottom: 18 }}>
        Reviewers below are skilled in <strong>{mcq.stackName}</strong>. The creator is excluded to prevent self-review.
      </p>
      <label className="field-label">Reviewer</label>
      {reviewers === null ? (
        <div className="skeleton-row" />
      ) : reviewers.length === 0 ? (
        <div className="review-comment" style={{ borderLeftColor: 'var(--amber)', background: 'var(--amber-soft)', color: '#8a5a16' }}>
          No other SME matches this stack. Add a skill mapping or assign another admin.
        </div>
      ) : (
        <select className="select" value={selected} onChange={(e) => setSelected(e.target.value)} autoFocus>
          <option value="">Select a reviewer…</option>
          {reviewers.map((r) => <option key={r.enterpriseId} value={r.enterpriseId}>{r.name} — {r.enterpriseId}</option>)}
        </select>
      )}
      <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Cancel</button>
        <button className="btn btn-accent" onClick={assign} disabled={busy || !selected}>
          {busy ? 'Assigning…' : 'Assign → Under Review'}
        </button>
      </div>
    </Modal>
  )
}

function BulkAssignDialog({ ids, onClose, onAssigned }: { ids: number[]; onClose: () => void; onAssigned: () => void }) {
  const [reviewers, setReviewers] = useState<EligibleReviewer[] | null>(null)
  const [selected, setSelected] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  useEffect(() => {
    api.bulkEligibleReviewers(ids).then(setReviewers).catch((e) => {
      toast('error', e instanceof ApiError ? e.message : 'Could not load reviewers'); setReviewers([])
    })
  }, [])

  async function assign() {
    if (!selected) return
    setBusy(true)
    try {
      const results = await api.bulkAssignReviewer(ids, selected)
      const ok = results.filter((r) => r.success).length
      const fail = results.length - ok
      toast(fail === 0 ? 'success' : 'info',
        `${ok} assigned to ${selected}${fail ? `, ${fail} could not be assigned` : ''}.`)
      onAssigned()
    } catch (e) { toast('error', e instanceof ApiError ? e.message : 'Bulk assignment failed') }
    finally { setBusy(false) }
  }

  return (
    <Modal open onClose={onClose} title="Assign Reviewer to Selection"
      subtitle={`${ids.length} question${ids.length === 1 ? '' : 's'} selected`} width={560}>
      <p style={{ color: 'var(--ink-soft)', fontSize: 14.5, marginBottom: 18 }}>
        Only reviewers skilled in <strong>every</strong> selected question's stack are listed,
        and the creator of any selected question is excluded — so the whole batch can be assigned safely.
      </p>
      <label className="field-label">Reviewer</label>
      {reviewers === null ? (
        <div className="skeleton-row" />
      ) : reviewers.length === 0 ? (
        <div className="review-comment" style={{ borderLeftColor: 'var(--amber)', background: 'var(--amber-soft)', color: '#8a5a16' }}>
          No single reviewer is eligible for all selected questions. Narrow the selection
          (e.g. pick questions of one technology stack) and try again.
        </div>
      ) : (
        <select className="select" value={selected} onChange={(e) => setSelected(e.target.value)} autoFocus>
          <option value="">Select a reviewer…</option>
          {reviewers.map((r) => <option key={r.enterpriseId} value={r.enterpriseId}>{r.name} — {r.enterpriseId}</option>)}
        </select>
      )}
      <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Cancel</button>
        <button className="btn btn-accent" onClick={assign} disabled={busy || !selected || (reviewers?.length ?? 0) === 0}>
          {busy ? 'Assigning…' : `Assign ${ids.length} → Under Review`}
        </button>
      </div>
    </Modal>
  )
}
