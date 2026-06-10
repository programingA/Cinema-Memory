"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowLeft, Clock, Film } from "lucide-react";
import { ProjectorScene } from "@/components/ProjectorScene";
import { getAccessToken, verifyAuthSession } from "@/lib/auth";
import { getPlaybackFilm, isApiError } from "@/lib/api";
import { findDemoPlaybackFilm } from "@/lib/mock-data";
import type { PlaybackFilm } from "@/lib/types";

type Props = {
  filmId: number;
};

export function PlaybackClient({ filmId }: Props) {
  const [film, setFilm] = useState<PlaybackFilm | null>(null);
  const [isReady, setIsReady] = useState(false);
  const [requiresLogin, setRequiresLogin] = useState(false);

  useEffect(() => {
    let cancelled = false;

    void verifyAuthSession().then((me) => {
      if (cancelled) {
        return;
      }

      if (!me) {
        const demoFilm = findDemoPlaybackFilm(filmId);
        queueMicrotask(() => {
          if (!cancelled) {
            setRequiresLogin(!demoFilm);
            setFilm(demoFilm ?? null);
            setIsReady(true);
          }
        });
        return;
      }

      void getPlaybackFilm(getAccessToken(), filmId)
        .then((serverFilm) => {
          if (!cancelled) {
            setFilm(serverFilm);
            setIsReady(true);
          }
        })
        .catch((error) => {
          if (!cancelled) {
            if (isApiError(error) && error.status === 401) {
              setRequiresLogin(true);
              setFilm(null);
              setIsReady(true);
              return;
            }

            const demoFilm = isApiError(error) && error.status === 404
              ? findDemoPlaybackFilm(filmId)
              : undefined;
            setFilm(demoFilm ?? null);
            setIsReady(true);
          }
        });
    });

    return () => {
      cancelled = true;
    };
  }, [filmId]);

  if (!isReady) {
    return (
      <main className="grid min-h-screen place-items-center px-5 py-6">
        <p className="text-sm text-stone-400">상영관을 준비하고 있습니다.</p>
      </main>
    );
  }

  if (requiresLogin) {
    return (
      <main className="grid min-h-screen place-items-center px-5 py-6">
        <section className="max-w-md rounded-lg border border-white/10 bg-black/45 p-6 text-center shadow-reel">
          <p className="text-sm font-semibold uppercase tracking-[0.22em] text-projector">Auth Required</p>
          <h1 className="mt-3 text-2xl font-semibold text-white">로그인이 필요합니다.</h1>
          <p className="mt-3 text-sm leading-6 text-stone-400">
            저장된 개인 필름은 서버에서 검증된 토큰으로만 열 수 있습니다.
          </p>
          <Link
            href="/"
            className="mt-5 inline-flex items-center justify-center gap-2 rounded-md bg-projector px-4 py-2.5 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
          >
            <ArrowLeft size={16} />
            로그인하러 가기
          </Link>
        </section>
      </main>
    );
  }

  if (!film) {
    return (
      <main className="grid min-h-screen place-items-center px-5 py-6">
        <section className="max-w-md rounded-lg border border-white/10 bg-black/45 p-6 text-center shadow-reel">
          <p className="text-sm font-semibold uppercase tracking-[0.22em] text-projector">Film Not Found</p>
          <h1 className="mt-3 text-2xl font-semibold text-white">저장된 필름을 찾을 수 없습니다.</h1>
          <p className="mt-3 text-sm leading-6 text-stone-400">
            현재 로그인한 사용자 저장소에 이 필름이 없거나 삭제된 필름입니다.
          </p>
          <Link
            href="/dashboard"
            className="mt-5 inline-flex items-center justify-center gap-2 rounded-md bg-projector px-4 py-2.5 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
          >
            <ArrowLeft size={16} />
            필름 보관함으로 이동
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen px-5 py-6 sm:px-8 lg:px-10">
      <div className="mx-auto max-w-7xl">
        <div className="mb-5 flex flex-col justify-between gap-4 rounded-lg border border-white/10 bg-black/40 p-4 shadow-reel sm:flex-row sm:items-center">
          <Link
            href="/dashboard"
            className="inline-flex w-fit items-center gap-2 rounded-md border border-white/10 px-3 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            <ArrowLeft size={16} />
            대시보드
          </Link>
          <div className="text-left sm:text-right">
            <p className="text-sm text-projector">Now Projecting</p>
            <h1 className="text-2xl font-semibold text-white">{film.title}</h1>
            <div className="mt-2 flex flex-wrap gap-2 text-xs text-stone-400 sm:justify-end">
              <span className="inline-flex items-center gap-1 rounded bg-white/[0.06] px-2 py-1">
                <Film size={13} />
                {film.sceneCount} scenes
              </span>
              <span className="inline-flex items-center gap-1 rounded bg-white/[0.06] px-2 py-1">
                <Clock size={13} />
                {film.mood}
              </span>
            </div>
          </div>
        </div>
        <ProjectorScene scenes={film.scenes} />
      </div>
    </main>
  );
}
