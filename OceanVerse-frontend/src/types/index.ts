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
  longitude?: number
  latitude?: number
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
  username?: string
  content: string
  postType: string
  relatedSpeciesId?: number
  relatedObservationId?: number
  imageUrls?: string
  likeCount: number
  commentCount: number
  favoriteCount: number
  status: number
  createTime: string
  updateTime?: string
  isLiked?: boolean
  isFavorited?: boolean
}

export interface CommunityComment {
  id: number
  postId: number
  userId: number
  username?: string
  parentId?: number
  content: string
  likeCount: number
  status: number
  createTime: string
}

export interface CommunityNotification {
  id: number
  userId: number
  title: string
  content: string
  type: string
  isRead: number
  relatedId?: number
  targetPostId?: number
  fromUserId?: number
  createTime: string
}

export interface UserProfile {
  userId: number
  username: string
  avatarUrl?: string
  postCount: number
  followerCount: number
  followCount: number
}

export interface PostCreateDTO {
  content: string
  postType?: string
  relatedSpeciesId?: number
  relatedObservationId?: number
  imageUrls?: string
}

export interface PostQueryDTO {
  postType?: string
  userId?: number
  speciesId?: number
  orderBy?: string
  page?: number
  size?: number
}

export interface CommentCreateDTO {
  postId: number
  parentId?: number
  content: string
}

// ==================== 生态观测模块 ====================

export interface Observation {
  id?: number
  observationCode: string
  observationType: string
  observationDate: string
  observationTime?: string
  durationMinutes?: number
  locationId?: number
  ecosystemId?: number
  latitude?: number
  longitude?: number
  depth?: number
  waterTemperature?: number
  salinity?: number
  ph?: number
  weatherCondition?: string
  seaCondition?: string
  observerId?: number
  observerName?: string
  organization?: string
  equipmentUsed?: string
  notes?: string
  createTime?: string
  updateTime?: string
}

export interface ObservationQueryDTO {
  keyword?: string
  observationType?: string
  startDate?: string
  endDate?: string
  ecosystemId?: number
  locationId?: number
  observerName?: string
  sort?: string
  page?: number
  size?: number
}

export interface ObservationStatistics {
  totalCount: number
  byType: Record<string, number>
  byEcosystem: Record<string, number>
  recentCount: number
  avgDuration?: number
  thisMonthCount?: number
  thisYearCount?: number
  avgWaterTemperature?: number
  avgSalinity?: number
}

export interface ObservationLocation {
  id?: number
  locationCode: string
  locationName: string
  locationType?: string
  latitude?: number
  longitude?: number
  country?: string
  province?: string
  city?: string
  ecosystemId?: number
  createTime?: string
  updateTime?: string
}

export interface Ecosystem {
  id?: number
  ecosystemCode: string
  ecosystemName: string
  ecosystemType?: string
  description?: string
  areaEstimate?: number
  depthMin?: number
  depthMax?: number
  temperatureRange?: string
  threatFactors?: string
  conservationStatus?: string
  createTime?: string
  updateTime?: string
}
