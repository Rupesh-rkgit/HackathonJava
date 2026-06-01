import type { McqStatus } from '../api/types'
import { STATUS_LABEL } from '../api/types'

export function StatusPill({ status }: { status: McqStatus }) {
  return (
    <span className={`pill ${status}`}>
      <span className="dot" />
      {STATUS_LABEL[status]}
    </span>
  )
}
