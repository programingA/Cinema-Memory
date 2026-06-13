"use client";

import { type ChangeEvent, type FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, CalendarDays, Film, ImagePlus, Save, Tags } from "lucide-react";
import {
  createFilm,
  createScene,
  deleteScene,
  getPlaybackFilm,
  updateFilm,
  updateScene as updateSceneRequest,
  uploadSceneMedia
} from "@/lib/api";
import { getAccessToken, verifyAuthSession } from "@/lib/auth";
import type { PlaybackFilm, SceneRequest } from "@/lib/types";

type SceneDraft = {
  sceneId?: number;
  title: string;
  body: string;
  memoryDate: string;
  tagsText: string;
  mediaDataUrl: string;
  mediaName: string;
  existingMediaUrl: string;
  mediaFile: File | null;
};

type SceneErrors = {
  title?: string;
  body?: string;
  media?: string;
};

type FilmErrors = {
  title?: string;
  description?: string;
  mood?: string;
  scenes?: SceneErrors[];
};

type Props = {
  editFilmId?: number;
};

const MAX_FILE_SIZE = 50 * 1024 * 1024;
const ALLOWED_MEDIA_TYPES = new Set([
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/gif",
  "image/webp",
  "video/mp4",
  "video/webm",
  "video/ogg",
  "video/quicktime"
]);

const MEDIA_TYPE_BY_EXTENSION: Record<string, string> = {
  gif: "image/gif",
  jpeg: "image/jpeg",
  jpg: "image/jpeg",
  m4v: "video/mp4",
  mov: "video/quicktime",
  mp4: "video/mp4",
  ogg: "video/ogg",
  png: "image/png",
  webm: "video/webm",
  webp: "image/webp"
};

const emptyScene = (): SceneDraft => ({
  title: "",
  body: "",
  memoryDate: new Date().toISOString().slice(0, 10),
  tagsText: "",
  mediaDataUrl: "",
  mediaName: "",
  existingMediaUrl: "",
  mediaFile: null
});

function inputClass(hasError: boolean) {
  return `rounded-md border bg-black/45 px-3 py-3 text-white outline-none transition focus:border-projector ${
    hasError ? "border-red-400/80" : "border-white/10"
  }`;
}

function FieldError({ message }: { message?: string }) {
  return message ? <span className="text-xs font-medium text-red-400">{message}</span> : null;
}

function mediaLabel(dataUrl: string) {
  if (!dataUrl) {
    return "";
  }

  return dataUrl.startsWith("data:video/") ? "기존 동영상" : "기존 이미지";
}

function inferMediaType(file: File) {
  const browserType = file.type.trim().toLowerCase();
  if (ALLOWED_MEDIA_TYPES.has(browserType)) {
    return browserType === "image/jpg" ? "image/jpeg" : browserType;
  }

  const extension = file.name.split(".").pop()?.trim().toLowerCase() ?? "";
  return MEDIA_TYPE_BY_EXTENSION[extension];
}

export function CreateFilmClient({ editFilmId }: Props) {
  const router = useRouter();
  const [isReady, setIsReady] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [mood, setMood] = useState("시네마틱");
  const [sceneCount, setSceneCount] = useState(1);
  const [scenes, setScenes] = useState<SceneDraft[]>([emptyScene()]);
  const [errors, setErrors] = useState<FilmErrors>({});
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [existingSceneIds, setExistingSceneIds] = useState<number[]>([]);

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

      if (!editFilmId) {
        queueMicrotask(() => {
          if (!cancelled) {
            setIsReady(true);
          }
        });
        return;
      }

      let film: PlaybackFilm;
      try {
        film = await getPlaybackFilm(getAccessToken(), editFilmId);
      } catch {
        router.replace("/dashboard");
        return;
      }

      queueMicrotask(() => {
        if (cancelled) {
          return;
        }

        setTitle(film.title);
        setDescription(film.description);
        setMood(film.mood ?? "");
        setSceneCount(film.scenes.length);
        setExistingSceneIds(film.scenes.map((scene) => scene.id));
        setScenes(
          film.scenes.map((scene) => {
            const mediaDataUrl = scene.mediaUrls[0] ?? "";
            return {
              sceneId: scene.id,
              title: scene.title,
              body: scene.body,
              memoryDate: scene.memoryDate ?? film.createdAt.slice(0, 10),
              tagsText: scene.tags?.join(", ") ?? "",
              mediaDataUrl,
              mediaName: mediaLabel(mediaDataUrl),
              existingMediaUrl: mediaDataUrl,
              mediaFile: null
            };
          })
        );
        setIsReady(true);
      });
    });

    return () => {
      cancelled = true;
    };
  }, [editFilmId, router]);

  function updateSceneCount(nextCount: number) {
    setSceneCount(nextCount);
    setScenes((current) => {
      if (nextCount > current.length) {
        return [...current, ...Array.from({ length: nextCount - current.length }, emptyScene)];
      }

      return current.slice(0, nextCount);
    });
  }

  function updateScene(index: number, patch: Partial<SceneDraft>) {
    setScenes((current) => current.map((scene, sceneIndex) => (
      sceneIndex === index ? { ...scene, ...patch } : scene
    )));
  }

  function onFileChange(index: number, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    const mediaType = inferMediaType(file);
    if (!mediaType) {
      setErrors((current) => ({
        ...current,
        scenes: scenes.map((_, sceneIndex) => sceneIndex === index
          ? { ...(current.scenes?.[sceneIndex] ?? {}), media: "JPG, PNG, GIF, WebP 이미지 또는 MP4, MOV, WebM, OGG 동영상만 업로드할 수 있습니다." }
          : current.scenes?.[sceneIndex] ?? {})
      }));
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setErrors((current) => ({
        ...current,
        scenes: scenes.map((_, sceneIndex) => sceneIndex === index
          ? { ...(current.scenes?.[sceneIndex] ?? {}), media: "미디어 파일은 50MB 이하만 업로드할 수 있습니다." }
          : current.scenes?.[sceneIndex] ?? {})
      }));
      return;
    }

    updateScene(index, {
      mediaDataUrl: `selected:${mediaType}:${file.name}`,
      mediaName: file.name,
      mediaFile: file
    });
    setErrors((current) => ({
      ...current,
      scenes: scenes.map((_, sceneIndex) => sceneIndex === index
        ? { ...(current.scenes?.[sceneIndex] ?? {}), media: undefined }
        : current.scenes?.[sceneIndex] ?? {})
    }));
  }

  function validate() {
    const sceneErrors = scenes.map((scene) => {
      const error: SceneErrors = {};

      if (!scene.title.trim()) {
        error.title = "장면 제목을 입력해주세요.";
      } else if (scene.title.trim().length < 2) {
        error.title = "장면 제목은 2자 이상이어야 합니다.";
      }

      if (!scene.body.trim()) {
        error.body = "장면 내용을 입력해주세요.";
      } else if (scene.body.trim().length < 10) {
        error.body = "장면 내용은 10자 이상 입력해주세요.";
      }

      if (!scene.mediaDataUrl) {
        error.media = "상영할 이미지 또는 동영상 파일을 업로드해주세요.";
      }

      return error;
    });

    const nextErrors: FilmErrors = { scenes: sceneErrors };

    if (!title.trim()) {
      nextErrors.title = "필름 제목을 입력해주세요.";
    } else if (title.trim().length < 2) {
      nextErrors.title = "필름 제목은 2자 이상이어야 합니다.";
    }

    if (!description.trim()) {
      nextErrors.description = "필름 설명을 입력해주세요.";
    } else if (description.trim().length < 10) {
      nextErrors.description = "설명은 10자 이상 입력해주세요.";
    }

    if (!mood.trim()) {
      nextErrors.mood = "분위기를 입력해주세요.";
    }

    setErrors(nextErrors);

    return !nextErrors.title
      && !nextErrors.description
      && !nextErrors.mood
      && sceneErrors.every((scene) => !scene.title && !scene.body && !scene.media);
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!validate()) {
      setSubmitError("입력값을 다시 확인해주세요.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError("");

    const me = await verifyAuthSession();
    if (!me) {
      setSubmitError("로그인 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
      setIsSubmitting(false);
      router.replace("/");
      window.dispatchEvent(new Event("cinema-memory:open-login"));
      return;
    }

    try {
      const accessToken = getAccessToken();
      const filmPayload = {
        title: title.trim(),
        description: description.trim(),
        coverImageUrl: scenes[0]?.existingMediaUrl || undefined,
        mood: mood.trim()
      };
      const savedFilm = editFilmId
        ? await updateFilm(accessToken, editFilmId, filmPayload)
        : await createFilm(accessToken, filmPayload);
      const filmId = savedFilm.id;
      const savedSceneIds = new Set<number>();
      let coverImageUrl = scenes[0]?.existingMediaUrl || savedFilm.coverImageUrl;

      for (const [index, scene] of scenes.entries()) {
        const scenePayload: SceneRequest = {
          title: scene.title.trim(),
          body: scene.body.trim(),
          memoryDate: scene.memoryDate || undefined,
          location: "",
          mood: mood.trim(),
          sortOrder: index + 1
        };
        const savedScene = scene.sceneId
          ? await updateSceneRequest(accessToken, filmId, scene.sceneId, scenePayload)
          : await createScene(accessToken, filmId, scenePayload);

        savedSceneIds.add(savedScene.id);

        if (scene.mediaFile) {
          const media = await uploadSceneMedia(accessToken, savedScene.id, scene.mediaFile);
          if (index === 0) {
            coverImageUrl = media.cdnUrl;
          }
        }
      }

      for (const sceneId of existingSceneIds) {
        if (!savedSceneIds.has(sceneId)) {
          await deleteScene(accessToken, filmId, sceneId);
        }
      }

      if (coverImageUrl && coverImageUrl !== savedFilm.coverImageUrl) {
        await updateFilm(accessToken, filmId, {
          ...filmPayload,
          coverImageUrl
        });
      }

      router.push(`/films/${filmId}/playback`);
    } catch {
      setSubmitError("서버 데이터베이스에 저장하지 못했습니다. 백엔드와 미디어 업로드 설정을 확인해주세요.");
      setIsSubmitting(false);
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
    <main className="min-h-screen px-5 py-10 sm:px-8 lg:px-10">
      <div className="mx-auto max-w-5xl">
        <button
          type="button"
          onClick={() => router.back()}
          className="mb-5 inline-flex items-center gap-2 rounded-md border border-white/10 px-3 py-2 text-sm text-stone-200 transition hover:bg-white/10"
        >
          <ArrowLeft size={16} />
          돌아가기
        </button>

        <section className="rounded-lg border border-white/10 bg-black/45 p-6 shadow-reel">
          <div className="mb-6 flex items-center gap-3">
            <span className="grid h-11 w-11 place-items-center rounded-md bg-projector text-stone-950">
              <Film size={20} />
            </span>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-projector">
                {editFilmId ? "Edit Film" : "New Film"}
              </p>
              <h1 className="text-3xl font-semibold text-white">{editFilmId ? "필름 수정" : "필름 만들기"}</h1>
            </div>
          </div>

          <form className="grid gap-5" onSubmit={onSubmit} noValidate>
            <label className="grid gap-2 text-sm text-stone-300">
              필름 제목
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                className={inputClass(Boolean(errors.title))}
                placeholder="예: 첫 여행의 밤"
              />
              <FieldError message={errors.title} />
            </label>

            <label className="grid gap-2 text-sm text-stone-300">
              설명
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                className={`${inputClass(Boolean(errors.description))} min-h-24`}
                placeholder="이 필름 안에 담을 추억을 설명하세요."
              />
              <FieldError message={errors.description} />
            </label>

            <div className="grid gap-5 md:grid-cols-2">
              <label className="grid gap-2 text-sm text-stone-300">
                분위기
                <input
                  value={mood}
                  onChange={(event) => setMood(event.target.value)}
                  className={inputClass(Boolean(errors.mood))}
                  placeholder="예: 따뜻함, 설렘, 그리움"
                />
                <FieldError message={errors.mood} />
              </label>

              <label className="grid gap-2 text-sm text-stone-300">
                추가할 장면 개수
                <select
                  value={sceneCount}
                  onChange={(event) => updateSceneCount(Number(event.target.value))}
                  className="rounded-md border border-white/10 bg-black/45 px-3 py-3 text-white outline-none focus:border-projector"
                >
                  {[1, 2, 3, 4, 5].map((count) => (
                    <option key={count} value={count}>{count}개</option>
                  ))}
                </select>
              </label>
            </div>

            <div className="grid gap-4">
              {scenes.map((scene, index) => {
                const sceneError = errors.scenes?.[index] ?? {};

                return (
                  <section key={index} className="rounded-lg border border-white/10 bg-stone-950/70 p-4">
                    <div className="mb-4 flex items-center justify-between gap-4">
                      <h2 className="font-semibold text-white">장면 {index + 1}</h2>
                      <span className="text-xs text-stone-500">이미지 또는 동영상</span>
                    </div>

                    <div className="grid gap-4">
                      <label className="grid gap-2 text-sm text-stone-300">
                        장면 제목
                        <input
                          value={scene.title}
                          onChange={(event) => updateScene(index, { title: event.target.value })}
                          className={inputClass(Boolean(sceneError.title))}
                          placeholder="예: 매표소 앞"
                        />
                        <FieldError message={sceneError.title} />
                      </label>

                      <label className="grid gap-2 text-sm text-stone-300">
                        장면 내용
                        <textarea
                          value={scene.body}
                          onChange={(event) => updateScene(index, { body: event.target.value })}
                          className={`${inputClass(Boolean(sceneError.body))} min-h-24`}
                          placeholder="이 장면에 얽힌 기억과 감정을 적어주세요."
                        />
                        <FieldError message={sceneError.body} />
                      </label>

                      <div className="grid gap-4 md:grid-cols-2">
                        <label className="grid gap-2 text-sm text-stone-300">
                          장면 날짜
                          <span className="flex items-center gap-2 rounded-md border border-white/10 bg-black/45 px-3 py-3 transition focus-within:border-projector">
                            <CalendarDays size={16} className="text-stone-500" />
                            <input
                              type="date"
                              value={scene.memoryDate}
                              onChange={(event) => updateScene(index, { memoryDate: event.target.value })}
                              className="w-full bg-transparent text-white outline-none"
                            />
                          </span>
                        </label>

                        <label className="grid gap-2 text-sm text-stone-300">
                          장면 태그
                          <span className="flex items-center gap-2 rounded-md border border-white/10 bg-black/45 px-3 py-3 transition focus-within:border-projector">
                            <Tags size={16} className="text-stone-500" />
                            <input
                              value={scene.tagsText}
                              onChange={(event) => updateScene(index, { tagsText: event.target.value })}
                              className="w-full bg-transparent text-white outline-none"
                              placeholder="예: 여행, 극장, 가족"
                            />
                          </span>
                        </label>
                      </div>

                      <label className="grid gap-2 text-sm text-stone-300">
                        미디어 업로드
                        <span className={`flex items-center gap-3 rounded-md border bg-black/45 px-3 py-3 ${sceneError.media ? "border-red-400/80" : "border-white/10"}`}>
                          <ImagePlus size={18} className="text-stone-500" />
                          <input
                            type="file"
                            accept=".jpg,.jpeg,.png,.gif,.webp,.mp4,.mov,.m4v,.webm,.ogg,image/jpeg,image/png,image/gif,image/webp,video/mp4,video/webm,video/ogg,video/quicktime"
                            onChange={(event) => void onFileChange(index, event)}
                            className="w-full text-sm text-stone-300 file:mr-3 file:rounded file:border-0 file:bg-projector file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-stone-950"
                          />
                        </span>
                        {scene.mediaName && <span className="text-xs text-stone-400">선택됨: {scene.mediaName}</span>}
                        <FieldError message={sceneError.media} />
                      </label>
                    </div>
                  </section>
                );
              })}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center justify-center gap-2 rounded-md bg-projector px-4 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-200 disabled:cursor-not-allowed disabled:opacity-70"
            >
              <Save size={16} />
              {editFilmId ? "수정 저장 후 상영" : "필름 저장 후 상영"}
            </button>
            {submitError && (
              <div className="rounded-md border border-red-400/30 bg-red-950/30 px-3 py-2 text-sm text-red-300">
                {submitError}
              </div>
            )}
          </form>
        </section>
      </div>
    </main>
  );
}
