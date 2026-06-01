import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { AnswerOption, Difficulty, Mcq, McqRequest, NamedRef, SaveMode } from '../api/types'
import './mcqform.css'

interface Props {
  initial?: Mcq | null
  /** Hide "Save & Send for Review" (e.g. admin super-edit just saves). */
  allowSend?: boolean
  submitting?: boolean
  onSubmit: (body: McqRequest, mode: SaveMode) => void
  onCancel?: () => void
}

const OPTIONS: AnswerOption[] = ['A', 'B', 'C', 'D']
const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD']

export function McqForm({ initial, allowSend = true, submitting, onSubmit, onCancel }: Props) {
  const [stacks, setStacks] = useState<NamedRef[]>([])
  const [topics, setTopics] = useState<NamedRef[]>([])
  const [stem, setStem] = useState(initial?.questionStem ?? '')
  const [opts, setOpts] = useState({
    A: initial?.optionA ?? '', B: initial?.optionB ?? '',
    C: initial?.optionC ?? '', D: initial?.optionD ?? '',
  })
  const [correct, setCorrect] = useState<AnswerOption>(initial?.correctAnswer ?? 'A')
  const [difficulty, setDifficulty] = useState<Difficulty>(initial?.difficulty ?? 'MEDIUM')
  const [stackId, setStackId] = useState<number | ''>(initial?.stackId ?? '')
  const [topicId, setTopicId] = useState<number | ''>(initial?.topicId ?? '')
  const [err, setErr] = useState('')

  useEffect(() => { api.stacks().then(setStacks).catch(() => {}) }, [])
  useEffect(() => {
    if (stackId === '') { setTopics([]); return }
    api.topics(Number(stackId)).then((t) => {
      setTopics(t)
      if (!t.some((x) => x.id === topicId)) setTopicId(initial && initial.stackId === stackId ? initial.topicId : '')
    }).catch(() => {})
  }, [stackId])

  function build(): McqRequest | null {
    if (!stem.trim()) return fail('Question stem is required.')
    if (!opts.A.trim() || !opts.B.trim() || !opts.C.trim() || !opts.D.trim())
      return fail('All four options are required.')
    if (stackId === '') return fail('Select a technology stack.')
    if (topicId === '') return fail('Select a topic.')
    return {
      questionStem: stem.trim(),
      optionA: opts.A.trim(), optionB: opts.B.trim(), optionC: opts.C.trim(), optionD: opts.D.trim(),
      correctAnswer: correct, difficulty, stackId: Number(stackId), topicId: Number(topicId),
      mode: 'SAVE',
    }
  }
  function fail(m: string): null { setErr(m); return null }

  function go(mode: SaveMode) {
    setErr('')
    const body = build()
    if (body) onSubmit({ ...body, mode }, mode)
  }

  return (
    <div className="mcqform">
      <div className="field">
        <label className="field-label">Question stem</label>
        <textarea
          className="textarea" rows={3} value={stem}
          placeholder="Describe the scenario and ask the question…"
          onChange={(e) => setStem(e.target.value)}
        />
      </div>

      <div className="opt-grid">
        {OPTIONS.map((o) => (
          <div key={o} className={`opt-row ${correct === o ? 'is-correct' : ''}`}>
            <button
              type="button"
              className={`opt-mark ${correct === o ? 'on' : ''}`}
              onClick={() => setCorrect(o)}
              title="Mark as correct answer"
            >
              {o}
            </button>
            <input
              className="input" value={opts[o]}
              placeholder={`Option ${o}`}
              onChange={(e) => setOpts((p) => ({ ...p, [o]: e.target.value }))}
            />
            {correct === o && <span className="opt-flag">Correct</span>}
          </div>
        ))}
      </div>
      <p className="opt-hint">Click a letter to mark the correct answer.</p>

      <div className="meta-grid">
        <div className="field">
          <label className="field-label">Technology stack</label>
          <select className="select" value={stackId} onChange={(e) => setStackId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">Select stack…</option>
            {stacks.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label className="field-label">Topic</label>
          <select className="select" value={topicId} disabled={stackId === ''} onChange={(e) => setTopicId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">{stackId === '' ? 'Pick a stack first' : 'Select topic…'}</option>
            {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label className="field-label">Difficulty</label>
          <select className="select" value={difficulty} onChange={(e) => setDifficulty(e.target.value as Difficulty)}>
            {DIFFICULTIES.map((d) => <option key={d} value={d}>{d[0] + d.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
      </div>

      {err && <div className="mcqform-err">{err}</div>}

      <div className="mcqform-actions">
        {onCancel && <button className="btn btn-ghost" onClick={onCancel} disabled={submitting}>Cancel</button>}
        <div className="spacer" />
        <button className="btn btn-ghost" onClick={() => go('SAVE')} disabled={submitting}>
          {submitting ? 'Saving…' : 'Save'}
        </button>
        {allowSend && (
          <button className="btn btn-accent" onClick={() => go('SAVE_AND_SEND')} disabled={submitting}>
            Save &amp; Send for Review →
          </button>
        )}
      </div>
    </div>
  )
}
