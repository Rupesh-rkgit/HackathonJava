import type { McqStatus } from '../api/types'
import './lifecycle.css'

const STEPS: { key: McqStatus; short: string }[] = [
  { key: 'DRAFT', short: 'Draft' },
  { key: 'READY_FOR_REVIEW', short: 'Ready' },
  { key: 'UNDER_REVIEW', short: 'Review' },
  { key: 'APPROVED', short: 'Approved' },
]

const ORDER: McqStatus[] = ['DRAFT', 'READY_FOR_REVIEW', 'UNDER_REVIEW', 'APPROVED']

/**
 * The signature motif: a horizontal rail showing where an MCQ sits in the
 * approval pipeline. Rejected is rendered as a diverted branch off "Review".
 */
export function LifecyclePipeline({ status, compact = false }: { status: McqStatus; compact?: boolean }) {
  const rejected = status === 'REJECTED'
  const activeIdx = rejected ? 2 : ORDER.indexOf(status)

  return (
    <div className={`pipeline ${compact ? 'compact' : ''}`} role="img" aria-label={`Lifecycle stage: ${status}`}>
      {STEPS.map((step, i) => {
        const done = i < activeIdx
        const current = i === activeIdx && !rejected
        const reviewRejected = rejected && step.key === 'UNDER_REVIEW'
        return (
          <div className="pl-step" key={step.key}>
            <div
              className={[
                'pl-node',
                done ? 'done' : '',
                current ? 'current' : '',
                reviewRejected ? 'rejected' : '',
              ].join(' ')}
            >
              <span className="pl-num">{reviewRejected ? '!' : done ? '✓' : i + 1}</span>
            </div>
            <span className="pl-label">{reviewRejected ? 'Rejected' : step.short}</span>
            {i < STEPS.length - 1 && <span className={`pl-bar ${i < activeIdx ? 'filled' : ''}`} />}
          </div>
        )
      })}
    </div>
  )
}
