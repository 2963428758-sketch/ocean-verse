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
  id?: number
  speciesCode: string
  scientificName: string
  commonName?: string
  chineseName?: string
  protectionLevel?: string
  iucnStatus?: string
  kingdom?: string
  phylum?: string
  className?: string
  orderName?: string
  family?: string
  genus?: string
  species?: string
  description?: string
  morphology?: string
  ecology?: string
  videoUrl?: string
  conservationStatusId?: number
  isEndemic?: number
  isInvasive?: number
  dataQuality?: string
  source?: string
  createTime?: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

export interface SpeciesDistribution {
  id?: number
  speciesId: number
  distributionType: string
  regionName?: string
  country?: string
  province?: string
  latitude?: number
  longitude?: number
  depthMin?: number
  depthMax?: number
  habitatType?: string
  isPrimary?: number
  populationEstimate?: string
  distributionStatus?: string
  createTime?: string
  updateTime?: string
}

export interface SpeciesMedia {
  id?: number
  speciesId: number
  mediaType: string
  fileName: string
  fileUrl: string
  fileSize?: number
  thumbnailUrl?: string
  mediaTitle?: string
  mediaDescription?: string
  isPrimary?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface SpeciesStatistics {
  totalCount: number
  byProtectionLevel: Record<string, number>
  byIucnStatus: Record<string, number>
  byFamily: Record<string, number>
  endemicCount: number
  invasiveCount: number
}

export interface SpeciesQueryDTO {
  keyword?: string
  family?: string
  iucnStatus?: string
  protectionLevel?: string
  sort?: string
  page?: number
  size?: number
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
