import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { McqRequest, SaveMode } from '../api/types'
import { McqForm } from '../components/McqForm'
import { BulkUpload } from './BulkUpload'
import { useToast } from '../components/Toast'
import './add.css'

type Mode = 'ui' | 'bulk'

export default function AddQuestion() {
  const [mode, setMode] = useState<Mode>('ui')
  const [saving, setSaving] = useState(false)
  const toast = useToast()
  const navigate = useNavigate()

  async function submit(body: McqRequest, saveMode: SaveMode) {
    setSaving(true)
    try {
      await api.createMcq(body)
      toast('success', saveMode === 'SAVE_AND_SEND'
        ? 'Question created and sent for review.'
        : 'Question saved as draft.')
      navigate('/questions')
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Could not save question')
    } finally { setSaving(false) }
  }

  return (
    <div>
      <div className="page-head">
        <span className="page-eyebrow">Authoring</span>
        <h1 className="page-title">Add a <em>Question</em></h1>
        <p className="page-sub">Create a single MCQ through the form, or import a batch from an Excel template.</p>
      </div>

      <div className="mode-switch">
        <button className={`ms-btn ${mode === 'ui' ? 'on' : ''}`} onClick={() => setMode('ui')}>
          <span className="ms-ico">✎</span>
          <span><strong>Add from UI</strong><em>One question at a time</em></span>
        </button>
        <button className={`ms-btn ${mode === 'bulk' ? 'on' : ''}`} onClick={() => setMode('bulk')}>
          <span className="ms-ico">⇪</span>
          <span><strong>Bulk Upload</strong><em>Many via Excel</em></span>
        </button>
      </div>

      <div className="card" style={{ padding: 28 }}>
        {mode === 'ui'
          ? <McqForm submitting={saving} onSubmit={submit} onCancel={() => navigate('/questions')} />
          : <BulkUpload />}
      </div>
    </div>
  )
}
