"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { CalendarDays, ChevronLeft, ChevronRight, MapPin, Pause, Play, Projector, Tags, X } from "lucide-react";
import type { MemoryScene } from "@/lib/types";

type Props = {
  scenes: MemoryScene[];
};

type SlideDirection = -1 | 1;

function isVideoMedia(url?: string) {
  if (!url) {
    return false;
  }

  return url.startsWith("data:video/") || /\.(mp4|webm|ogg)(\?|$)/i.test(url);
}

export function ProjectorScene({ scenes }: Props) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(false);
  const [incomingIndex, setIncomingIndex] = useState<number | null>(null);
  const [isSlideAnimating, setIsSlideAnimating] = useState(false);
  const [slideDirection, setSlideDirection] = useState<SlideDirection>(1);
  const transitionStartTimerRef = useRef<number | null>(null);
  const transitionEndTimerRef = useRef<number | null>(null);
  const safeActiveIndex = scenes[activeIndex] ? activeIndex : 0;
  const activeScene = scenes[safeActiveIndex];
  const incomingScene = incomingIndex === null ? null : scenes[incomingIndex] ?? null;
  const progress = useMemo(() => `${safeActiveIndex + 1} / ${scenes.length}`, [safeActiveIndex, scenes.length]);

  const transitionToScene = useCallback((nextIndex: number, direction: SlideDirection = 1) => {
    if (scenes.length === 0 || nextIndex === safeActiveIndex) {
      return;
    }

    if (transitionStartTimerRef.current) {
      window.clearTimeout(transitionStartTimerRef.current);
    }

    if (transitionEndTimerRef.current) {
      window.clearTimeout(transitionEndTimerRef.current);
    }

    setSlideDirection(direction);
    setIncomingIndex(nextIndex);
    setIsSlideAnimating(false);

    transitionStartTimerRef.current = window.setTimeout(() => {
      setIsSlideAnimating(true);
    }, 20);

    transitionEndTimerRef.current = window.setTimeout(() => {
      setActiveIndex(nextIndex);
      setIncomingIndex(null);
      setIsSlideAnimating(false);
    }, 720);
  }, [safeActiveIndex, scenes.length]);

  useEffect(() => {
    return () => {
      if (transitionStartTimerRef.current) {
        window.clearTimeout(transitionStartTimerRef.current);
      }

      if (transitionEndTimerRef.current) {
        window.clearTimeout(transitionEndTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!isAutoPlaying || scenes.length <= 1) {
      return;
    }

    const timerId = window.setTimeout(() => {
      transitionToScene((safeActiveIndex + 1) % scenes.length, 1);
    }, 4200);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [isAutoPlaying, scenes.length, safeActiveIndex, transitionToScene]);

  useEffect(() => {
    if (!isAutoPlaying) {
      return;
    }

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsAutoPlaying(false);
      }
    };

    window.addEventListener("keydown", closeOnEscape);
    return () => {
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [isAutoPlaying]);

  if (!activeScene) {
    return (
      <section className="grid min-h-[520px] place-items-center rounded-lg border border-white/10 bg-black text-stone-300">
        상영할 장면이 없습니다.
      </section>
    );
  }

  const slideTransitionClass = incomingScene ? "transition-[transform,filter] duration-700 ease-in-out" : "";
  const outgoingSlideClass = incomingScene
    ? isSlideAnimating
      ? slideDirection === 1 ? "-translate-x-full blur-[2px]" : "translate-x-full blur-[2px]"
      : "translate-x-0 blur-0"
    : "translate-x-0 blur-0";
  const incomingSlideClass = incomingScene
    ? isSlideAnimating
      ? "translate-x-0 blur-0"
      : slideDirection === 1 ? "translate-x-full blur-[2px]" : "-translate-x-full blur-[2px]"
    : "translate-x-full blur-[2px]";
  const sceneTextClass = `transition duration-700 ease-in-out ${
    incomingScene && isSlideAnimating
      ? slideDirection === 1 ? "-translate-x-8 opacity-0" : "translate-x-8 opacity-0"
      : "translate-x-0 opacity-100"
  }`;

  function move(delta: number) {
    transitionToScene((safeActiveIndex + delta + scenes.length) % scenes.length, delta < 0 ? -1 : 1);
  }

  function stopSlideshow() {
    setIsAutoPlaying(false);
  }

  function renderSceneMedia(scene: MemoryScene, isFullscreen: boolean) {
    const mediaUrl = scene.mediaUrls[0];
    const isVideo = isVideoMedia(mediaUrl);

    if (mediaUrl && isVideo) {
      return (
        <video
          key={mediaUrl}
          src={mediaUrl}
          className="h-full w-full bg-black object-contain opacity-95"
          controls={!isFullscreen}
          muted={isFullscreen}
          autoPlay={isFullscreen}
          playsInline
        />
      );
    }

    if (mediaUrl) {
      return (
        <div
          key={mediaUrl}
          className={`h-full w-full bg-center opacity-95 ${isFullscreen ? "bg-contain bg-no-repeat" : "bg-cover"}`}
          style={{ backgroundImage: `url(${mediaUrl})` }}
        />
      );
    }

    return <div className="h-full w-full bg-gradient-to-br from-projector/30 via-sky-300/20 to-velvet/40 opacity-95" />;
  }

  return (
    <>
      <section className="relative overflow-hidden rounded-lg border border-white/10 bg-[#050505] shadow-screen">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_68%_20%,rgba(246,211,101,0.18),transparent_34%),linear-gradient(135deg,#070606,#15110f_48%,#050505)]" />
        <div className="pointer-events-none absolute inset-x-0 top-0 h-28 bg-gradient-to-b from-velvet/28 to-transparent" />
        <div className="pointer-events-none absolute left-[28%] top-[33%] hidden h-52 w-[44%] -skew-y-6 bg-gradient-to-r from-projector/28 via-projector/10 to-transparent blur-xl md:block" />

        <div className="relative z-10 grid min-h-[720px] gap-6 p-5 lg:grid-cols-[330px_1fr] lg:p-8">
          <aside className="flex flex-col justify-between rounded-lg border border-white/10 bg-black/52 p-5 backdrop-blur">
            <div>
              <div className="mb-5 flex items-center gap-3">
                <span className="grid h-11 w-11 place-items-center rounded-md bg-projector text-stone-950">
                  <Projector size={20} />
                </span>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-projector">Projector</p>
                  <h2 className="font-semibold text-white">상영 장비</h2>
                </div>
              </div>

              <div className="relative mx-auto mb-6 h-64 max-w-[280px]">
                <div className="absolute left-8 top-4 h-24 w-24 animate-[spin_8s_linear_infinite] rounded-full border-[14px] border-stone-300/90 bg-stone-950 shadow-reel">
                  <div className="absolute left-1/2 top-1/2 h-5 w-5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-stone-500" />
                </div>
                <div className="absolute right-8 top-9 h-20 w-20 animate-[spin_7s_linear_infinite_reverse] rounded-full border-[11px] border-stone-500 bg-stone-950">
                  <div className="absolute left-1/2 top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full bg-stone-300" />
                </div>
                <div className="absolute bottom-14 left-6 h-24 w-48 rounded-lg border border-white/10 bg-gradient-to-br from-stone-800 to-stone-950 shadow-reel">
                  <div className="absolute left-5 top-5 h-5 w-20 rounded-full bg-projector/50 blur-sm" />
                  <div className="absolute bottom-4 left-5 h-3 w-28 rounded-full bg-stone-600" />
                  <div className="absolute right-[-38px] top-8 h-12 w-20 rounded-r-full border border-white/10 bg-gradient-to-r from-stone-600 to-stone-800" />
                  <div className="absolute right-[-52px] top-11 h-6 w-10 rounded-r-full bg-projector/80 blur-sm" />
                </div>
                <div className="absolute bottom-0 left-16 h-16 w-3 rounded-full bg-stone-700" />
                <div className="absolute bottom-0 left-32 h-16 w-3 rounded-full bg-stone-700" />
              </div>

              <div className="grid gap-3 text-sm text-stone-300">
                <div className="rounded-md border border-white/10 bg-white/[0.03] p-3">
                  <div className="text-xs uppercase tracking-[0.18em] text-stone-500">Current Reel</div>
                  <div className="mt-1 font-medium text-white">{progress}</div>
                </div>
                {activeScene.memoryDate && (
                  <div className="flex items-center gap-2 rounded-md border border-white/10 bg-white/[0.03] p-3">
                    <CalendarDays size={15} className="text-projector" />
                    {activeScene.memoryDate}
                  </div>
                )}
                {activeScene.location && (
                  <div className="flex items-center gap-2 rounded-md border border-white/10 bg-white/[0.03] p-3">
                    <MapPin size={15} className="text-projector" />
                    {activeScene.location}
                  </div>
                )}
                {activeScene.tags && activeScene.tags.length > 0 && (
                  <div className="rounded-md border border-white/10 bg-white/[0.03] p-3">
                    <div className="mb-2 flex items-center gap-2 text-xs uppercase tracking-[0.18em] text-stone-500">
                      <Tags size={14} className="text-projector" />
                      Tags
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {activeScene.tags.map((tag) => (
                        <span key={tag} className="rounded bg-projector/10 px-2 py-1 text-xs text-projector">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="mt-6 grid gap-2">
              {scenes.map((scene, index) => (
                <button
                  key={scene.id}
                  type="button"
                  onClick={() => transitionToScene(index, index < safeActiveIndex ? -1 : 1)}
                  className={`flex items-center justify-between rounded-md border px-3 py-2 text-left text-sm transition ${
                    index === safeActiveIndex
                      ? "border-projector/70 bg-projector/12 text-white"
                      : "border-white/10 bg-white/[0.03] text-stone-400 hover:text-white"
                  }`}
                >
                  <span className="line-clamp-1">{scene.title}</span>
                  <span className="text-xs text-projector">{index + 1}</span>
                </button>
              ))}
            </div>
          </aside>

          <div className="flex min-w-0 flex-col justify-between gap-5">
            <div className="screen-frame relative overflow-hidden rounded-lg border border-white/15 bg-stone-100 p-3 shadow-screen">
              <div className="film-strip absolute left-0 top-0 z-10 h-full w-8 opacity-55" />
              <div className="film-strip absolute right-0 top-0 z-10 h-full w-8 opacity-55" />
              <div className="relative aspect-[16/9] overflow-hidden rounded-md bg-stone-950">
                <div className={`absolute inset-0 ${slideTransitionClass} ${outgoingSlideClass}`}>
                  {renderSceneMedia(activeScene, false)}
                </div>
                {incomingScene && (
                  <div className={`absolute inset-0 ${slideTransitionClass} ${incomingSlideClass}`}>
                    {renderSceneMedia(incomingScene, false)}
                  </div>
                )}
                <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/78 via-black/8 to-transparent" />
                <div className="pointer-events-none absolute left-5 top-5 inline-flex items-center gap-2 rounded-md bg-black/55 px-3 py-1 text-xs font-medium text-projector backdrop-blur">
                  <Play size={13} />
                  Scene {progress}
                </div>
                <div className={`pointer-events-none absolute bottom-6 left-6 right-6 ${sceneTextClass}`}>
                  <p className="mb-2 text-sm font-medium text-projector">{activeScene.mood}</p>
                  <h1 className="text-3xl font-semibold text-white sm:text-5xl">{activeScene.title}</h1>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-white/10 bg-black/56 p-5 backdrop-blur">
              <p className={`max-w-3xl text-base leading-7 text-stone-200 sm:text-lg ${sceneTextClass}`}>{activeScene.body}</p>
              <div className="mt-5 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  onClick={() => move(-1)}
                  className="inline-flex items-center gap-2 rounded-md border border-white/10 px-3 py-2 text-sm text-stone-200 transition hover:bg-white/10"
                >
                  <ChevronLeft size={16} />
                  이전
                </button>
                <button
                  type="button"
                  onClick={() => move(1)}
                  className="inline-flex items-center gap-2 rounded-md bg-projector px-3 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
                >
                  다음
                  <ChevronRight size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => setIsAutoPlaying(true)}
                  disabled={scenes.length <= 1}
                  className="inline-flex items-center gap-2 rounded-md border border-projector/40 px-3 py-2 text-sm font-semibold text-projector transition hover:bg-projector/10 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Play size={16} />
                  슬라이드쇼 재생
                </button>
                <div className="flex flex-wrap gap-2">
                  {scenes.map((scene, index) => (
                    <button
                      key={scene.id}
                      type="button"
                      onClick={() => transitionToScene(index, index < safeActiveIndex ? -1 : 1)}
                      className={`h-2.5 w-12 rounded-full transition ${
                        index === safeActiveIndex ? "bg-projector" : "bg-white/30 hover:bg-white/60"
                      }`}
                      aria-label={`${scene.title} 장면으로 이동`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {isAutoPlaying && (
        <div className="fixed inset-0 z-[90] overflow-hidden bg-black">
          <div className="absolute inset-0 bg-stone-950">
            <div className={`absolute inset-0 ${slideTransitionClass} ${outgoingSlideClass}`}>
              {renderSceneMedia(activeScene, true)}
            </div>
            {incomingScene && (
              <div className={`absolute inset-0 ${slideTransitionClass} ${incomingSlideClass}`}>
                {renderSceneMedia(incomingScene, true)}
              </div>
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/88 via-black/22 to-black/42" />
          </div>

          <div className="relative z-10 min-h-screen p-5 sm:p-8">
            <div className="absolute left-5 right-5 top-5 flex items-center justify-between gap-4 sm:left-8 sm:right-8 sm:top-8">
              <div className="inline-flex items-center gap-2 rounded-md bg-black/55 px-3 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-projector backdrop-blur">
                <Play size={14} />
                Scene {progress}
              </div>
              <button
                type="button"
                onClick={stopSlideshow}
                className="grid h-11 w-11 place-items-center rounded-md border border-white/15 bg-black/45 text-stone-200 transition hover:bg-white/10 hover:text-white"
                aria-label="슬라이드쇼 닫기"
              >
                <X size={19} />
              </button>
            </div>

            <div className={`absolute bottom-28 left-5 max-w-xl pr-5 sm:bottom-32 sm:left-8 ${sceneTextClass}`}>
              <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-projector drop-shadow">{activeScene.mood}</p>
              <h1 className="text-3xl font-semibold text-white drop-shadow-[0_2px_12px_rgba(0,0,0,0.95)] sm:text-5xl">{activeScene.title}</h1>
              <p className="mt-3 line-clamp-2 max-w-lg text-sm leading-6 text-stone-200 drop-shadow-[0_2px_10px_rgba(0,0,0,0.95)] sm:text-base">
                {activeScene.body}
              </p>
              {activeScene.tags && activeScene.tags.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {activeScene.tags.map((tag) => (
                    <span key={tag} className="rounded bg-black/45 px-2 py-1 text-xs text-projector">
                      #{tag}
                    </span>
                  ))}
                </div>
              )}
            </div>

            <div className="absolute bottom-5 left-5 right-5 flex flex-wrap items-center justify-between gap-4 rounded-lg border border-white/10 bg-black/48 p-3 backdrop-blur sm:bottom-8 sm:left-8 sm:right-8">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => move(-1)}
                  className="inline-flex items-center gap-2 rounded-md border border-white/10 px-3 py-2 text-sm text-stone-200 transition hover:bg-white/10"
                >
                  <ChevronLeft size={16} />
                  이전
                </button>
                <button
                  type="button"
                  onClick={() => move(1)}
                  className="inline-flex items-center gap-2 rounded-md bg-projector px-3 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
                >
                  다음
                  <ChevronRight size={16} />
                </button>
                <button
                  type="button"
                  onClick={stopSlideshow}
                  className="inline-flex items-center gap-2 rounded-md border border-projector/40 px-3 py-2 text-sm font-semibold text-projector transition hover:bg-projector/10"
                >
                  <Pause size={16} />
                  자동 재생 중지
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {scenes.map((scene, index) => (
                  <button
                    key={scene.id}
                    type="button"
                    onClick={() => transitionToScene(index, index < safeActiveIndex ? -1 : 1)}
                    className={`h-2.5 w-10 rounded-full transition ${
                      index === safeActiveIndex ? "bg-projector" : "bg-white/30 hover:bg-white/60"
                    }`}
                    aria-label={`${scene.title} 장면으로 이동`}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
