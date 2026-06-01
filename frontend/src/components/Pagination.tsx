import './common.css'

interface Props {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onChange }: Props) {
  if (totalPages <= 1) {
    return <div className="pager"><span className="pager-meta">{totalElements} item{totalElements === 1 ? '' : 's'}</span></div>
  }
  return (
    <div className="pager">
      <span className="pager-meta">{totalElements} items · page {page + 1} of {totalPages}</span>
      <div className="pager-btns">
        <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>← Prev</button>
        <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next →</button>
      </div>
    </div>
  )
}
