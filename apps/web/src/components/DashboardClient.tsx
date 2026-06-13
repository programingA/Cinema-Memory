"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Clapperboard, Edit3, Film, Play, Plus, Search, ShieldCheck, Trash2, Video, X } from "lucide-react";
import { AppDialog } from "@/components/AppDialog";
import { getAccessToken, verifyAuthSession } from "@/lib/auth";
import { deleteFilm as deleteFilmRequest, getFilms } from "@/lib/api";
import type { Film as FilmItem } from "@/lib/types";

function isVideoMedia(url?: string) {
  if (!url) {
    return false;
  }

  return url.startsWith("data:video/") || /\.(mp4|webm|ogg)(\?|$)/i.test(url);
}

function cssBackgroundImage(url: string) {
  return `url("${url.replace(/["\\\n\r\f]/g, "\\$&")}")`;
}

function getFilmSearchText(film: FilmItem) {
  return [
    film.title,
    film.description,
    film.mood,
    film.visibility
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

export function DashboardClient() {
  const router = useRouter();
  const [isReady, setIsReady] = useState(false);
  const [serverFilms, setServerFilms] = useState<FilmItem[]>([]);
  const [currentUser, setCurrentUserLabel] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState("");
  const [loadError, setLoadError] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<FilmItem | null>(null);
  const [isDeletingFilm, setIsDeletingFilm] = useState(false);

  useEffect(() => {
    let cancelled = false;

    void verifyAuthSession().then(async (me) => {
      if (cancelled) {
        return;
      }

      if (!me) {
        router.replace("/");
        window.dispatchEvent(new Event("cinema-memory:open-login"));
        return;
      }

      try {
        const films = await getFilms(getAccessToken());
        if (!cancelled) {
          setCurrentUserLabel(me.email);
          setServerFilms(films);
          setIsReady(true);
        }
      } catch {
        if (!cancelled) {
          setLoadError("Saved films could not be loaded from the server.");
          setCurrentUserLabel(me.email);
          setIsReady(true);
        }
      }
    });

    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
    }, 300);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [searchQuery]);

  const normalizedSearchQuery = debouncedSearchQuery.toLowerCase();
  const films = useMemo(() => {
    if (!normalizedSearchQuery) {
      return serverFilms;
    }

    return serverFilms.filter((film) => getFilmSearchText(film).includes(normalizedSearchQuery));
  }, [serverFilms, normalizedSearchQuery]);
  const totalSceneCount = useMemo(
    () => serverFilms.reduce((count, film) => count + film.sceneCount, 0),
    [serverFilms]
  );
  const isSearching = debouncedSearchQuery.length > 0;

  function deleteFilm(filmId: number) {
    const film = serverFilms.find((item) => item.id === filmId);
    if (film) {
      setDeleteTarget(film);
    }
  }

  async function confirmDeleteFilm() {
    if (!deleteTarget) {
      return;
    }

    setIsDeletingFilm(true);
    try {
      await deleteFilmRequest(getAccessToken(), deleteTarget.id);
      setServerFilms((current) => current.filter((film) => film.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch {
      setLoadError("The film could not be deleted from the server.");
    } finally {
      setIsDeletingFilm(false);
    }
  }

  if (!isReady) {
    return (
      <main className="grid min-h-[60vh] place-items-center px-5">
        <div className="rounded-lg border border-white/10 bg-black/45 px-5 py-4 text-sm text-stone-300">
          서버에서 토큰을 검증하고 있습니다...
        </div>
      </main>
    );
  }

  return (
    <>
    <main className="min-h-screen px-5 py-10 sm:px-8 lg:px-10">
      <div className="mx-auto grid max-w-7xl gap-8 lg:grid-cols-[18rem_minmax(0,1fr)]">
        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="rounded-lg border border-white/10 bg-stone-950/85 p-5 shadow-reel">
            <div className="flex items-center gap-2 text-projector">
              <ShieldCheck size={16} />
              <span className="truncate text-sm font-semibold">{currentUser || "verified user"}</span>
            </div>

            <div className="mt-5 border-t border-white/10 pt-5">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-stone-500">Dashboard</p>
              <div className="mt-3 grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-2xl font-semibold text-white">{serverFilms.length}</p>
                  <p className="mt-1 text-stone-500">필름</p>
                </div>
                <div>
                  <p className="text-2xl font-semibold text-white">{totalSceneCount}</p>
                  <p className="mt-1 text-stone-500">장면</p>
                </div>
              </div>
            </div>

            <div className="mt-6">
              <label htmlFor="dashboard-search" className="text-sm font-medium text-stone-300">
                상영관 검색
              </label>
              <div className="relative mt-2">
                <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-stone-500" />
                <input
                  id="dashboard-search"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  className="h-11 w-full rounded-md border border-white/10 bg-black/45 pl-10 pr-10 text-sm text-white outline-none transition placeholder:text-stone-600 focus:border-projector"
                  placeholder="제목, 설명, 장면 검색"
                />
                {searchQuery && (
                  <button
                    type="button"
                    aria-label="검색어 지우기"
                    onClick={() => setSearchQuery("")}
                    className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center rounded-md text-stone-400 transition hover:bg-white/10 hover:text-white"
                  >
                    <X size={15} />
                  </button>
                )}
              </div>
              <p className="mt-2 text-xs leading-5 text-stone-500">
                {isSearching ? `${films.length}개의 검색 결과가 있습니다.` : "입력 후 0.3초 뒤 검색 결과를 갱신합니다."}
              </p>
            </div>

            <div className="mt-6 grid gap-2">
              <Link
                href="/films/new"
                className="inline-flex items-center justify-center gap-2 rounded-md bg-projector px-4 py-2.5 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
              >
                <Plus size={16} />
                필름 만들기
              </Link>
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          {loadError && (
            <div className="mb-5 rounded-lg border border-red-400/30 bg-red-950/20 px-4 py-3 text-sm text-red-100">
              {loadError}
            </div>
          )}

          <section className="mb-8 border-b border-white/10 pb-7">
            <div className="mb-3 inline-flex items-center gap-2 rounded-md bg-projector/10 px-3 py-1 text-xs font-semibold text-projector">
              <Clapperboard size={14} />
              Screening Room
            </div>
            <h1 className="text-3xl font-semibold text-white sm:text-4xl">내 필름 보관함</h1>
            <p className="mt-3 max-w-2xl text-stone-300">
              백엔드에서 검증된 사용자 이메일을 기준으로 로컬 필름 저장소를 열고, 저장된 상영관을 빠르게 찾습니다.
            </p>
          </section>

          <section>
            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-projector">
                <Film size={18} />
                <h2 className="text-lg font-semibold">내 필름</h2>
              </div>
              <span className="text-sm text-stone-500">
                {isSearching ? `${films.length} / ${serverFilms.length}개 표시` : `${serverFilms.length}개 표시`}
              </span>
            </div>

            {serverFilms.length === 0 ? (
              <div className="rounded-lg border border-white/10 bg-stone-950/70 p-8 text-center">
                <Film className="mx-auto text-projector" size={34} />
                <h3 className="mt-4 text-xl font-semibold text-white">아직 만든 필름이 없습니다</h3>
                <p className="mt-2 text-sm text-stone-400">
                  필름 만들기에서 이미지나 동영상을 업로드해 첫 상영을 만들어보세요.
                </p>
                <Link
                  href="/films/new"
                  className="mt-5 inline-flex items-center gap-2 rounded-md bg-projector px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
                >
                  <Plus size={16} />
                  필름 만들기
                </Link>
              </div>
            ) : films.length === 0 ? (
              <div className="rounded-lg border border-white/10 bg-stone-950/70 p-8 text-center">
                <Search className="mx-auto text-projector" size={34} />
                <h3 className="mt-4 text-xl font-semibold text-white">검색 결과가 없습니다</h3>
                <p className="mt-2 text-sm text-stone-400">
                  다른 제목, 설명, 장면 키워드로 다시 검색해보세요.
                </p>
                <button
                  type="button"
                  onClick={() => setSearchQuery("")}
                  className="mt-5 inline-flex items-center gap-2 rounded-md border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
                >
                  <X size={16} />
                  검색어 지우기
                </button>
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {films.map((film) => {
                  const coverIsVideo = isVideoMedia(film.coverImageUrl);

                  return (
                    <article
                      key={film.id}
                      className="group rounded-lg border border-white/10 bg-stone-950/80 p-4 transition hover:-translate-y-1 hover:border-projector/50"
                    >
                      <Link href={`/films/${film.id}/playback`}>
                        <div className="relative mb-4 aspect-video overflow-hidden rounded-md bg-stone-900">
                          {film.coverImageUrl && coverIsVideo ? (
                            <video src={film.coverImageUrl} className="h-full w-full object-cover opacity-80" muted playsInline />
                          ) : film.coverImageUrl ? (
                            <div className="h-full w-full bg-cover bg-center" style={{ backgroundImage: cssBackgroundImage(film.coverImageUrl) }} />
                          ) : (
                            <div className="grid h-full place-items-center text-stone-500">
                              <Film size={28} />
                            </div>
                          )}
                          {coverIsVideo && (
                            <span className="absolute left-3 top-3 inline-flex items-center gap-1 rounded bg-black/60 px-2 py-1 text-xs text-projector">
                              <Video size={13} />
                              Video
                            </span>
                          )}
                        </div>
                      </Link>
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <h3 className="truncate font-semibold text-white">{film.title}</h3>
                          <p className="mt-1 line-clamp-2 text-sm text-stone-400">{film.description}</p>
                          <p className="mt-2 text-xs text-stone-500">{film.sceneCount} scenes</p>
                        </div>
                        <Link href={`/films/${film.id}/playback`} className="rounded-md bg-projector p-2 text-stone-950" aria-label={`${film.title} 상영`}>
                          <Play size={15} />
                        </Link>
                      </div>
                      <div className="mt-4 grid grid-cols-2 gap-2">
                        <Link
                          href={`/films/${film.id}/edit`}
                          className="inline-grid h-10 place-items-center rounded-md border border-white/10 text-stone-200 transition hover:bg-white/10"
                          aria-label={`${film.title} 수정`}
                          title="수정"
                        >
                          <Edit3 size={14} />
                        </Link>
                        <button
                          type="button"
                          onClick={() => void deleteFilm(film.id)}
                          className="inline-grid h-10 place-items-center rounded-md border border-red-400/30 text-red-300 transition hover:bg-red-950/30"
                          aria-label={`${film.title} 삭제`}
                          title="삭제"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div>
    </main>

    <AppDialog
      open={Boolean(deleteTarget)}
      title="필름을 삭제할까요?"
      description={
        <>
          <span className="font-semibold text-white">{deleteTarget?.title}</span> 필름이 서버 데이터베이스에서 삭제됩니다.
        </>
      }
      confirmLabel={isDeletingFilm ? "삭제 중..." : "삭제"}
      cancelLabel="취소"
      tone="danger"
      isBusy={isDeletingFilm}
      onConfirm={() => void confirmDeleteFilm()}
      onClose={() => setDeleteTarget(null)}
    />
    </>
  );
}
