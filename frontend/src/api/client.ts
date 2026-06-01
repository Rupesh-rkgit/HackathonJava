import type {
  BulkRowResult, EligibleReviewer, LoginResponse, Mcq, McqRequest, NamedRef, Page,
} from './types'

const TOKEN_KEY = 'quizhub.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(t: string) { localStorage.setItem(TOKEN_KEY, t) }
export function clearToken() { localStorage.removeItem(TOKEN_KEY) }

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const res = await fetch(path, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
    if (!path.endsWith('/auth/login')) {
      throw new ApiError(res.status, 'Your session is not authorized. Please sign in again.')
    }
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch { /* keep default */ }
    throw new ApiError(res.status, message)
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T
  }
  const ct = res.headers.get('content-type') ?? ''
  if (!ct.includes('application/json')) return (await res.blob()) as unknown as T
  return res.json() as Promise<T>
}

export const api = {
  // — auth —
  login: (enterpriseId: string, password: string) =>
    request<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ enterpriseId, password }),
    }),

  // — master data —
  stacks: () => request<NamedRef[]>('/api/masterdata/stacks'),
  topics: (stackId: number) => request<NamedRef[]>(`/api/masterdata/topics?stackId=${stackId}`),

  // — my questions —
  myQuestions: (page = 0, size = 8) =>
    request<Page<Mcq>>(`/api/mcqs/mine?page=${page}&size=${size}&sort=id,desc`),
  getMcq: (id: number) => request<Mcq>(`/api/mcqs/${id}`),
  createMcq: (body: McqRequest) =>
    request<Mcq>('/api/mcqs', { method: 'POST', body: JSON.stringify(body) }),
  updateMcq: (id: number, body: McqRequest) =>
    request<Mcq>(`/api/mcqs/${id}`, { method: 'PUT', body: JSON.stringify(body) }),

  // — bulk —
  uploadBulk: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return request<BulkRowResult[]>('/api/bulk/upload', { method: 'POST', body: fd })
  },
  templateUrl: '/api/bulk/template',

  // — reviews —
  pendingReviews: (page = 0, size = 8) =>
    request<Page<Mcq>>(`/api/reviews/pending?page=${page}&size=${size}`),
  approve: (id: number) => request<void>(`/api/reviews/${id}/approve`, { method: 'POST' }),
  reject: (id: number, comments: string) =>
    request<void>(`/api/reviews/${id}/reject`, {
      method: 'POST', body: JSON.stringify({ comments }),
    }),

  // — admin —
  allMcqs: (page = 0, size = 8) =>
    request<Page<Mcq>>(`/api/admin/mcqs?page=${page}&size=${size}&sort=id,desc`),
  adminUpdateMcq: (id: number, body: McqRequest) =>
    request<Mcq>(`/api/admin/mcqs/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  eligibleReviewers: (mcqId: number) =>
    request<EligibleReviewer[]>(`/api/admin/mcqs/${mcqId}/eligible-reviewers`),
  assignReviewer: (mcqId: number, reviewerEnterpriseId: string) =>
    request<void>(`/api/admin/mcqs/${mcqId}/assign`, {
      method: 'POST', body: JSON.stringify({ reviewerEnterpriseId }),
    }),
}

/** Authenticated file download (template) honoring the bearer token. */
export async function downloadTemplate() {
  const res = await fetch(api.templateUrl, {
    headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
  })
  if (!res.ok) throw new ApiError(res.status, 'Could not download template')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'Template_MCQs.xlsx'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
