import { AnimatePresence, motion } from 'framer-motion'
import './bulkbar.css'

interface Props {
  count: number
  onClear: () => void
  actionLabel: string
  onAction: () => void
  busy?: boolean
  noun?: string
  /** Optional secondary action rendered before the primary one. */
  children?: React.ReactNode
}

export function BulkBar({ count, onClear, actionLabel, onAction, busy, noun = 'item', children }: Props) {
  return (
    <AnimatePresence>
      {count > 0 && (
        <motion.div
          className="bulkbar"
          initial={{ opacity: 0, y: 60, x: '-50%' }}
          animate={{ opacity: 1, y: 0, x: '-50%' }}
          exit={{ opacity: 0, y: 60, x: '-50%' }}
          transition={{ type: 'spring', stiffness: 320, damping: 30 }}
        >
          <span className="bb-count">
            <strong>{count}</strong> {noun}{count === 1 ? '' : 's'} selected
          </span>
          <button className="bb-clear" onClick={onClear} disabled={busy}>Clear</button>
          <div className="bb-spacer" />
          {children}
          <button className="btn btn-accent" onClick={onAction} disabled={busy}>
            {busy ? 'Working…' : actionLabel}
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
