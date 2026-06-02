export type Role = 'SME' | 'ADMIN'
export type McqStatus =
  | 'DRAFT' | 'READY_FOR_REVIEW' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type AnswerOption = 'A' | 'B' | 'C' | 'D'
export type SaveMode = 'SAVE' | 'SAVE_AND_SEND'

export interface LoginResponse {
  token: string
  role: Role
  enterpriseId: string
  name: string
}

export interface Mcq {
  id: number
  questionStem: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: AnswerOption
  difficulty: Difficulty
  stackId: number
  stackName: string
  topicId: number
  topicName: string
  creatorEnterpriseId: string
  status: McqStatus
  reviewerComments: string | null
}

export interface McqRequest {
  questionStem: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: AnswerOption
  difficulty: Difficulty
  stackId: number
  topicId: number
  mode: SaveMode
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface NamedRef { id: number; name: string }
export interface EligibleReviewer { enterpriseId: string; name: string }
export interface BulkRowResult { rowNumber: number; success: boolean; message: string }
export interface BulkActionResult { mcqId: number; success: boolean; message: string }

export const STATUS_LABEL: Record<McqStatus, string> = {
  DRAFT: 'Draft',
  READY_FOR_REVIEW: 'Ready for Review',
  UNDER_REVIEW: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
}

export const LIFECYCLE: McqStatus[] = [
  'DRAFT', 'READY_FOR_REVIEW', 'UNDER_REVIEW', 'APPROVED',
]
