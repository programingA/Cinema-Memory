import type { Film, PlaybackFilm } from "@/lib/types";

export const demoFilms: Film[] = [
  {
    id: 1,
    title: "봄날의 산책",
    description: "벚꽃 아래에서 남긴 조용한 오후의 기록",
    coverImageUrl: "https://images.unsplash.com/photo-1522383225653-ed111181a951?auto=format&fit=crop&w=1200&q=80",
    mood: "따뜻함",
    createdAt: "2026-04-01T10:00:00Z",
    sceneCount: 3
  },
  {
    id: 2,
    title: "여름 바다",
    description: "파도 소리와 필름 카메라 셔터음이 함께 있던 날",
    coverImageUrl: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
    mood: "청량함",
    createdAt: "2026-04-12T10:00:00Z",
    sceneCount: 3
  },
  {
    id: 3,
    title: "심야 영화",
    description: "늦은 밤 극장 조명과 엔딩 크레딧의 여운",
    coverImageUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80",
    mood: "시네마틱",
    createdAt: "2026-04-20T10:00:00Z",
    sceneCount: 3
  }
];

export const demoPlaybackFilm: PlaybackFilm = {
  ...demoFilms[0],
  scenes: [
    {
      id: 101,
      filmId: 1,
      title: "매표소 앞",
      body: "오래 기다린 하루가 시작되던 시간. 손에는 작은 꽃다발과 따뜻한 커피가 있었다.",
      memoryDate: "2026-04-01",
      location: "Seoul",
      mood: "기대감",
      sortOrder: 1,
      mediaUrls: [demoFilms[0].coverImageUrl ?? ""]
    },
    {
      id: 102,
      filmId: 1,
      title: "빛이 들어온 장면",
      body: "필름 가장자리처럼 흔들리던 햇빛이 사진 속에 천천히 남았다.",
      memoryDate: "2026-04-01",
      location: "Seoul",
      mood: "차분함",
      sortOrder: 2,
      mediaUrls: ["https://images.unsplash.com/photo-1490750967868-88aa4486c946?auto=format&fit=crop&w=1200&q=80"]
    },
    {
      id: 103,
      filmId: 1,
      title: "엔딩 크레딧",
      body: "돌아오는 길에도 같은 이야기를 몇 번이나 다시 꺼냈다.",
      memoryDate: "2026-04-01",
      location: "Seoul",
      mood: "그리움",
      sortOrder: 3,
      mediaUrls: ["https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80"]
    }
  ]
};

export const demoPlaybackFilms: PlaybackFilm[] = [
  demoPlaybackFilm,
  {
    ...demoFilms[1],
    scenes: [
      {
        id: 201,
        filmId: 2,
        title: "파도 앞에서",
        body: "해가 기울기 전, 모래 위에 남긴 발자국과 파도 소리가 한 장면처럼 남았다.",
        memoryDate: "2026-04-12",
        location: "Busan",
        mood: "청량함",
        sortOrder: 1,
        mediaUrls: [demoFilms[1].coverImageUrl ?? ""]
      },
      {
        id: 202,
        filmId: 2,
        title: "셔터 소리",
        body: "바람 때문에 머리카락이 계속 흩어졌지만, 그래서 더 자연스러운 사진이 되었다.",
        memoryDate: "2026-04-12",
        location: "Busan",
        mood: "자유로움",
        sortOrder: 2,
        mediaUrls: ["https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80"]
      },
      {
        id: 203,
        filmId: 2,
        title: "푸른 엔딩",
        body: "돌아오는 길 창밖으로 바다가 멀어질 때까지 아무 말 없이 바라봤다.",
        memoryDate: "2026-04-12",
        location: "Busan",
        mood: "여운",
        sortOrder: 3,
        mediaUrls: ["https://images.unsplash.com/photo-1471922694854-ff1b63b20054?auto=format&fit=crop&w=1200&q=80"]
      }
    ]
  },
  {
    ...demoFilms[2],
    scenes: [
      {
        id: 301,
        filmId: 3,
        title: "극장 입구",
        body: "늦은 밤의 붉은 조명과 매표소 불빛이 하루의 마지막 장면처럼 보였다.",
        memoryDate: "2026-04-20",
        location: "Seoul",
        mood: "시네마틱",
        sortOrder: 1,
        mediaUrls: [demoFilms[2].coverImageUrl ?? ""]
      },
      {
        id: 302,
        filmId: 3,
        title: "상영 전 정적",
        body: "불이 꺼지기 전 잠깐의 조용함 속에서 서로의 표정을 확인했다.",
        memoryDate: "2026-04-20",
        location: "Seoul",
        mood: "설렘",
        sortOrder: 2,
        mediaUrls: ["https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1200&q=80"]
      },
      {
        id: 303,
        filmId: 3,
        title: "엔딩 이후",
        body: "크레딧이 끝난 뒤에도 자리를 바로 뜨지 못하고 마지막 음악을 들었다.",
        memoryDate: "2026-04-20",
        location: "Seoul",
        mood: "잔상",
        sortOrder: 3,
        mediaUrls: ["https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?auto=format&fit=crop&w=1200&q=80"]
      }
    ]
  }
];

export function findDemoPlaybackFilm(filmId: number) {
  return demoPlaybackFilms.find((film) => film.id === filmId);
}
