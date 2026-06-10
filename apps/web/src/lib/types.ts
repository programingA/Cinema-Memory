export type Film = {
  id: number;
  title: string;
  description: string;
  coverImageUrl?: string;
  mood?: string;
  visibility?: string;
  createdAt: string;
  sceneCount: number;
};

export type MemoryScene = {
  id: number;
  filmId: number;
  title: string;
  body: string;
  memoryDate?: string;
  location?: string;
  mood?: string;
  tags?: string[];
  sortOrder: number;
  mediaUrls: string[];
};

export type PlaybackFilm = Film & {
  scenes: MemoryScene[];
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
};

export type MeResponse = {
  id: number;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
  role: UserRole;
  admin: boolean;
};

export type UserRole = "USER" | "ADMIN";

export type AdminSummary = {
  userCount: number;
  adminCount: number;
  filmCount: number;
  sceneCount: number;
  mediaCount: number;
};

export type AdminUser = {
  id: number;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
  role: UserRole;
  createdAt: string;
};

export type AdminFilm = {
  id: number;
  title: string;
  ownerEmail: string;
  ownerDisplayName: string;
  visibility: string;
  createdAt: string;
  sceneCount: number;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type SignupRequest = LoginRequest & {
  displayName: string;
};

export type FilmRequest = {
  title: string;
  description?: string;
  coverImageUrl?: string;
  mood?: string;
};

export type SceneRequest = {
  title: string;
  body: string;
  memoryDate?: string;
  location?: string;
  mood?: string;
  sortOrder: number;
};

export type PresignedUrlResponse = {
  uploadUrl: string;
  s3Key: string;
  cdnUrl: string;
};

export type MediaResponse = {
  id: number;
  cdnUrl: string;
};
