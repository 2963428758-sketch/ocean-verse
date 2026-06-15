export interface Result<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface User {
  id: number
  username: string
  email: string
  phone?: string
  realName?: string
  avatarUrl?: string
  status: number
  createTime: string
}

export interface Species {
  id: number
  speciesCode: string
  scientificName: string
  commonName?: string
  chineseName?: string
  protectionLevel?: string
  iucnStatus?: string
  family?: string
  description?: string
  morphology?: string
  ecology?: string
}

export interface CommunityPost {
  id: number
  userId: number
  content: string
  postType: string
  imageUrls?: string
  likeCount: number
  commentCount: number
  createTime: string
}
