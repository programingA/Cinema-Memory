import type { PlaybackFilm } from "@/lib/types";

const CURRENT_USER_KEY = "cinema-memory.current-user";
const STORAGE_PREFIX = "cinema-memory.local-films";
const ANONYMOUS_STORAGE_KEY = `${STORAGE_PREFIX}:anonymous`;
const LEGACY_STORAGE_KEY = STORAGE_PREFIX;

export function setCurrentUser(email: string) {
  localStorage.setItem(CURRENT_USER_KEY, email.trim().toLowerCase());
}

export function getCurrentUser() {
  if (typeof window === "undefined") {
    return "";
  }

  return localStorage.getItem(CURRENT_USER_KEY) ?? "";
}

export function clearCurrentUser() {
  localStorage.removeItem(CURRENT_USER_KEY);
}

function storageKey() {
  const user = getCurrentUser();
  return user ? `${STORAGE_PREFIX}:${user}` : `${STORAGE_PREFIX}:anonymous`;
}

function readFilmsFromKey(key: string): PlaybackFilm[] {
  if (typeof window === "undefined") {
    return [];
  }

  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as PlaybackFilm[]) : [];
  } catch {
    return [];
  }
}

function writeFilmsToKey(key: string, films: PlaybackFilm[]) {
  localStorage.setItem(key, JSON.stringify(films));
}

function mergeFilms(target: PlaybackFilm[], incoming: PlaybackFilm[]) {
  const existingIds = new Set(target.map((film) => film.id));
  return [
    ...target,
    ...incoming.filter((film) => {
      if (existingIds.has(film.id)) {
        return false;
      }

      existingIds.add(film.id);
      return true;
    })
  ];
}

export function adoptAnonymousFilmsForCurrentUser() {
  if (typeof window === "undefined") {
    return;
  }

  try {
    const currentKey = storageKey();
    const currentUser = getCurrentUser();

    if (!currentUser || currentKey === ANONYMOUS_STORAGE_KEY) {
      return;
    }

    const currentFilms = readFilmsFromKey(currentKey);
    const anonymousFilms = readFilmsFromKey(ANONYMOUS_STORAGE_KEY);
    const legacyFilms = readFilmsFromKey(LEGACY_STORAGE_KEY);
    const mergedFilms = mergeFilms(mergeFilms(currentFilms, anonymousFilms), legacyFilms);

    if (mergedFilms.length !== currentFilms.length) {
      writeFilmsToKey(currentKey, mergedFilms);
    }

    if (anonymousFilms.length > 0) {
      localStorage.removeItem(ANONYMOUS_STORAGE_KEY);
    }

    if (legacyFilms.length > 0) {
      localStorage.removeItem(LEGACY_STORAGE_KEY);
    }
  } catch {
    // Local demo data migration should never block auth or page rendering.
  }
}

export function readLocalFilms(): PlaybackFilm[] {
  adoptAnonymousFilmsForCurrentUser();
  return readFilmsFromKey(storageKey());
}

function writeLocalFilms(films: PlaybackFilm[]) {
  writeFilmsToKey(storageKey(), films);
}

export function writeLocalFilm(film: PlaybackFilm) {
  const films = readLocalFilms();
  writeLocalFilms([film, ...films.filter((item) => item.id !== film.id)]);
}

export function updateLocalFilm(film: PlaybackFilm) {
  const films = readLocalFilms();
  writeLocalFilms(films.map((item) => item.id === film.id ? film : item));
}

export function deleteLocalFilm(filmId: number) {
  writeLocalFilms(readLocalFilms().filter((film) => film.id !== filmId));
}

export function findLocalFilm(filmId: number) {
  return readLocalFilms().find((film) => film.id === filmId);
}
