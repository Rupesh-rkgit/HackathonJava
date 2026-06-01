import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError, downloadTemplate } from '../api/client'
import type { BulkRowResult } from '../api/types'
import { useToast } from '../components/Toast'

export function BulkUpload() {
  const [drag, setDrag] = useState(false)
  const [busy, setBusy] = useState(false)
  const [fileName, setFileName] = useState('')
  const [results, setResults] = useState<BulkRowResult[] | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const toast = useToast()
  const navigate = useNavigate()

  async function handleFile(file: File) {
    setFileName(file.name)
    setBusy(true)
    setResults(null)
    try {
      const res = await api.uploadBulk(file)
      setResults(res)
      const ok = res.filter((r) => r.success).length
      toast(ok > 0 ? 'success' : 'info', `${ok} of ${res.length} rows imported as drafts.`)
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Upload failed')
    } finally { setBusy(false) }
  }

  async function getTemplate() {
    try { await downloadTemplate(); toast('info', 'Template downloaded.') }
    catch { toast('error', 'Could not download template.') }
  }

  const okCount = results?.filter((r) => r.success).length ?? 0
  const badCount = results ? results.length - okCount : 0

  return (
    <div>
      <div className="bulk-intro">
        <p>
          Upload a populated <code className="mono">Template_MCQs.xlsx</code>. Each row is validated
          (stack, topic, difficulty, stem, four options, correct answer). Valid rows are imported as
          <strong> drafts</strong> you can refine and send for review.
        </p>
        <button className="btn btn-ghost" onClick={getTemplate}>⤓ Download template</button>
      </div>

      <div
        className={`dropzone ${drag ? 'drag' : ''}`}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDrag(true) }}
        onDragLeave={() => setDrag(false)}
        onDrop={(e) => {
          e.preventDefault(); setDrag(false)
          const f = e.dataTransfer.files?.[0]; if (f) handleFile(f)
        }}
      >
        <div className="dz-glyph">⇪</div>
        <div className="dz-title">{busy ? 'Importing…' : 'Drop your Excel file here'}</div>
        <div className="dz-sub">or click to browse · .xlsx</div>
        {fileName && <div className="dz-file">{fileName}</div>}
        <input
          ref={inputRef} type="file" accept=".xlsx" hidden
          onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f) }}
        />
      </div>

      {results && (
        <div className="bulk-results">
          <div className="bulk-summary">
            <div className="bsum ok"><div className="bnum">{okCount}</div><div className="blabel">Imported</div></div>
            <div className="bsum bad"><div className="bnum">{badCount}</div><div className="blabel">Rejected rows</div></div>
          </div>
          <div className="card" style={{ overflow: 'hidden' }}>
            {results.map((r) => (
              <div key={r.rowNumber} className={`brow ${r.success ? 'ok' : 'bad'}`}>
                <span className="bmark">{r.success ? '✓' : '×'}</span>
                <span className="brow-no">Row {r.rowNumber}</span>
                <span>{r.message}</span>
              </div>
            ))}
          </div>
          {okCount > 0 && (
            <button className="btn btn-accent" style={{ marginTop: 18 }} onClick={() => navigate('/questions')}>
              View imported drafts in My Questions →
            </button>
          )}
        </div>
      )}
    </div>
  )
}
