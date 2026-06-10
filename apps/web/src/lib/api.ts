import type {
  AdminFilm,
  AdminSummary,
  AdminUser,
  AuthTokens,
  Film,
  FilmRequest,
  LoginRequest,
  MediaResponse,
  MeResponse,
  PlaybackFilm,
  SceneRequest,
  MemoryScene,
  SignupRequest,
  UserRole
} from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  body: string;

  constructor(status: number, body: string) {
    super(`API request failed: ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

function resolveAssetUrl(url?: string | null) {
  if (!url || /^(https?:|data:|blob:)/i.test(url)) {
    return url ?? undefined;
  }

  return `${API_BASE_URL}${url.startsWith("/") ? url : `/${url}`}`;
}

function normalizeFilm(film: Film): Film {
  return {
    ...film,
    coverImageUrl: resolveAssetUrl(film.coverImageUrl)
  };
}

function normalizePlaybackFilm(film: PlaybackFilm): PlaybackFilm {
  return {
    ...normalizeFilm(film),
    scenes: film.scenes.map((scene) => ({
      ...scene,
      mediaUrls: scene.mediaUrls.map((url) => resolveAssetUrl(url) ?? url)
    }))
  };
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers
    },
    cache: "no-store"
  });

  const text = await response.text();

  if (!response.ok) {
    throw new ApiError(response.status, text);
  }

  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export async function getFilms(accessToken: string): Promise<Film[]> {
  const films = await request<Film[]>("/films", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
  return films.map(normalizeFilm);
}

export async function getPlaybackFilm(accessToken: string, filmId: number): Promise<PlaybackFilm> {
  const film = await request<PlaybackFilm>(`/films/${filmId}/playback`, {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
  return normalizePlaybackFilm(film);
}

export async function createFilm(accessToken: string, payload: FilmRequest): Promise<Film> {
  return request<Film>("/films", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });
}

export async function updateFilm(accessToken: string, filmId: number, payload: FilmRequest): Promise<Film> {
  return request<Film>(`/films/${filmId}`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });
}

export async function deleteFilm(accessToken: string, filmId: number): Promise<void> {
  return request<void>(`/films/${filmId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function createScene(accessToken: string, filmId: number, payload: SceneRequest): Promise<MemoryScene> {
  return request<MemoryScene>(`/films/${filmId}/scenes`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });
}

export async function updateScene(
  accessToken: string,
  filmId: number,
  sceneId: number,
  payload: SceneRequest
): Promise<MemoryScene> {
  return request<MemoryScene>(`/films/${filmId}/scenes/${sceneId}`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });
}

export async function deleteScene(accessToken: string, filmId: number, sceneId: number): Promise<void> {
  return request<void>(`/films/${filmId}/scenes/${sceneId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function uploadSceneMedia(accessToken: string, sceneId: number, file: File): Promise<MediaResponse> {
  const formData = new FormData();
  formData.append("sceneId", String(sceneId));
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/media/upload`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: formData,
    cache: "no-store"
  });

  const text = await response.text();
  if (!response.ok) {
    throw new ApiError(response.status, text);
  }

  const media = JSON.parse(text) as MediaResponse;
  return {
    ...media,
    cdnUrl: resolveAssetUrl(media.cdnUrl) ?? media.cdnUrl
  };
}

export async function login(payload: LoginRequest): Promise<AuthTokens> {
  return request<AuthTokens>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function signup(payload: SignupRequest): Promise<AuthTokens> {
  return request<AuthTokens>("/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function refreshAuthSession(refreshToken: string): Promise<AuthTokens> {
  return request<AuthTokens>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken })
  });
}

export async function getMe(accessToken: string): Promise<MeResponse> {
  return request<MeResponse>("/auth/me", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function getAdminSummary(accessToken: string): Promise<AdminSummary> {
  return request<AdminSummary>("/admin/summary", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function getAdminUsers(accessToken: string): Promise<AdminUser[]> {
  return request<AdminUser[]>("/admin/users", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function getAdminFilms(accessToken: string): Promise<AdminFilm[]> {
  return request<AdminFilm[]>("/admin/films", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

export async function updateAdminUserRole(accessToken: string, userId: number, role: UserRole): Promise<AdminUser> {
  return request<AdminUser>(`/admin/users/${userId}/role`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify({ role })
  });
}

export const oauthUrl = (provider: "google" | "kakao") =>
  `${API_BASE_URL}/oauth2/authorization/${provider}`;
