import { getMe, isApiError, refreshAuthSession } from "@/lib/api";
import { adoptAnonymousFilmsForCurrentUser, clearCurrentUser, setCurrentUser } from "@/lib/local-films";
import type { AuthTokens, MeResponse } from "@/lib/types";

const ACCESS_TOKEN_KEY = "cinema-memory.access-token";
const REFRESH_TOKEN_KEY = "cinema-memory.refresh-token";

let refreshSessionPromise: Promise<AuthTokens | null> | null = null;

export function getAccessToken() {
  if (typeof window === "undefined") {
    return "";
  }

  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? "";
}

function getRefreshToken() {
  if (typeof window === "undefined") {
    return "";
  }

  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? "";
}

function persistTokenPair(tokens: AuthTokens) {
  if (!tokens.accessToken || !tokens.refreshToken) {
    throw new Error("Auth response does not include tokens.");
  }

  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

async function refreshStoredSession() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  refreshSessionPromise ??= refreshAuthSession(refreshToken)
    .then((tokens) => {
      persistTokenPair(tokens);
      return tokens;
    })
    .catch(() => null)
    .finally(() => {
      refreshSessionPromise = null;
    });

  return refreshSessionPromise;
}

export function clearAuthSession() {
  if (typeof window === "undefined") {
    return;
  }

  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  clearCurrentUser();
  window.dispatchEvent(new Event("cinema-memory:auth-changed"));
}

export function persistAuthSession(email: string, tokens: AuthTokens) {
  const normalizedEmail = email.trim().toLowerCase();
  persistTokenPair(tokens);
  setCurrentUser(normalizedEmail);

  try {
    adoptAnonymousFilmsForCurrentUser();
  } catch {
    // Local prototype data migration should not block a valid login.
  }

  window.dispatchEvent(new Event("cinema-memory:auth-changed"));
}

export async function persistVerifiedAuthSession(tokens: AuthTokens): Promise<MeResponse> {
  persistTokenPair(tokens);

  const me = await verifyAuthSession();
  if (!me) {
    throw new Error("Stored OAuth tokens are not valid.");
  }

  try {
    adoptAnonymousFilmsForCurrentUser();
  } catch {
    // Local prototype data migration should not block a valid login.
  }

  window.dispatchEvent(new Event("cinema-memory:auth-changed"));
  return me;
}

export async function verifyAuthSession(): Promise<MeResponse | null> {
  let accessToken = getAccessToken();

  if (!accessToken) {
    const refreshedTokens = await refreshStoredSession();
    if (!refreshedTokens) {
      clearAuthSession();
      return null;
    }

    accessToken = refreshedTokens.accessToken;
  }

  try {
    const me = await getMe(accessToken);
    setCurrentUser(me.email);
    return me;
  } catch (error) {
    if (!isApiError(error) || error.status !== 401) {
      return null;
    }
  }

  const refreshedTokens = await refreshStoredSession();
  if (!refreshedTokens) {
    clearAuthSession();
    return null;
  }

  try {
    const me = await getMe(refreshedTokens.accessToken);
    setCurrentUser(me.email);
    return me;
  } catch (error) {
    if (isApiError(error) && error.status === 401) {
      clearAuthSession();
    }

    return null;
  }
}
