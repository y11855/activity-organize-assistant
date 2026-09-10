/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  roles: string[]
}

/** 实训详情 */
export interface TrainingVO {
  id: number
  title: string
  fileUrl: string
  aiScore?: number
  teacherScore?: number
  finalScore?: number
  aiComment?: string
  status: number
  createTime: string
}

/** 评价指标 */
export interface EvaluationMetric {
  id: number
  name: string
  description: string
  weight: number
  /** 评分方式: ai | teacher */
  scoreType: 'ai' | 'teacher'
}

/** 统计报表 */
export interface ReportData {
  totalStudents: number
  totalTrainings: number
  averageScore: number
  scoreDistribution: { range: string; count: number }[]
}
