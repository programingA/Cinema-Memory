"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight, Clapperboard, Film, Projector, Sparkles } from "lucide-react";
import { FilmCard } from "@/components/FilmCard";
import { demoFilms } from "@/lib/mock-data";

const stats = [
  ["3", "상영 중인 필름"],
  ["9", "저장된 장면"],
  ["4K", "파노라마 감상"]
];

function openLoginModal() {
  window.dispatchEvent(new Event("cinema-memory:open-login"));
}

export function HomeExperience() {
  return (
    <main className="min-h-screen overflow-hidden">
      <section className="relative border-b border-white/10">
        <div className="curtain-bg absolute inset-0" />
        <div className="projector-sweep absolute inset-y-0 left-1/2 w-[55vw] -translate-x-1/2 opacity-70" />
        <div className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-7xl items-center gap-10 px-5 py-14 sm:px-8 lg:grid-cols-[1.04fr_0.96fr] lg:px-10">
          <motion.div
            initial={{ opacity: 0, y: 28 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="relative z-10"
          >
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-projector/30 bg-projector/10 px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.22em] text-projector">
              <Sparkles size={14} />
              Private Memory Theater
            </div>
            <h1 className="max-w-4xl text-5xl font-semibold leading-tight text-white sm:text-7xl">
              추억을 필름에 담고 다시 상영하세요
            </h1>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-stone-200">
              사진, 영상, 글, 분위기를 하나의 필름으로 묶고 영사기 화면에서 파노라마처럼 감상하는 개인 추억 보관함입니다.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={openLoginModal}
                className="inline-flex items-center gap-2 rounded-md bg-projector px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
              >
                이메일로 시작
                <ArrowRight size={16} />
              </button>
              <Link
                href="#projector"
                className="inline-flex items-center gap-2 rounded-md border border-white/10 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/10"
              >
                상영 미리보기
                <Projector size={16} />
              </Link>
            </div>

            <div className="mt-10 grid max-w-2xl grid-cols-3 gap-3">
              {stats.map(([value, label]) => (
                <motion.div
                  key={label}
                  initial={{ opacity: 0, y: 18 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.16, duration: 0.55 }}
                  className="rounded-lg border border-white/10 bg-black/38 p-4 backdrop-blur"
                >
                  <div className="text-2xl font-semibold text-projector">{value}</div>
                  <div className="mt-1 text-xs text-stone-300">{label}</div>
                </motion.div>
              ))}
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.96, rotateX: 5 }}
            animate={{ opacity: 1, scale: 1, rotateX: 0 }}
            transition={{ duration: 0.85, ease: "easeOut" }}
            className="relative z-10"
          >
            <div className="screen-frame relative overflow-hidden rounded-lg border border-white/15 bg-black p-3 shadow-screen">
              <div className="film-strip absolute left-0 top-0 h-full w-8 opacity-70" />
              <div className="film-strip absolute right-0 top-0 h-full w-8 opacity-70" />
              <div className="relative aspect-[4/3] overflow-hidden rounded-md bg-[url('https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1400&q=80')] bg-cover bg-center">
                <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-transparent" />
                <div className="absolute bottom-5 left-5 right-5">
                  <div className="mb-3 inline-flex items-center gap-2 rounded-md bg-black/52 px-3 py-1 text-xs text-projector backdrop-blur">
                    <Clapperboard size={14} />
                    오늘의 상영
                  </div>
                  <h2 className="text-3xl font-semibold text-white">심야 영화</h2>
                  <p className="mt-2 max-w-md text-sm leading-6 text-stone-200">
                    극장 조명부터 엔딩 크레딧까지, 장면마다 남은 감정을 필름처럼 이어 붙입니다.
                  </p>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <section id="films" className="relative px-5 py-16 sm:px-8 lg:px-10">
        <div className="mx-auto max-w-7xl">
          <div className="mb-8 flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.22em] text-projector">Film Archive</p>
              <h2 className="mt-2 text-3xl font-semibold text-white sm:text-4xl">예시 필름 보관함</h2>
              <p className="mt-3 max-w-2xl text-stone-300">
                아래 예시 필름은 로그인 없이도 상영 화면을 확인할 수 있습니다.
              </p>
            </div>
            <div className="inline-flex items-center gap-2 text-sm text-stone-400">
              <Film size={16} />
              데모 데이터 표시 중
            </div>
          </div>
          <div className="grid gap-5 md:grid-cols-3">
            {demoFilms.map((film, index) => (
              <FilmCard key={film.id} film={film} index={index} />
            ))}
          </div>
        </div>
      </section>

      <section id="projector" className="border-y border-white/10 bg-black/34 px-5 py-16 sm:px-8 lg:px-10">
        <div className="mx-auto grid max-w-7xl gap-8 lg:grid-cols-[0.9fr_1.1fr]">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.22em] text-projector">Projection Flow</p>
            <h2 className="mt-2 text-3xl font-semibold text-white sm:text-4xl">필름을 넣으면 장면이 열립니다</h2>
            <p className="mt-4 leading-7 text-stone-300">
              필름을 선택하면 영사기 화면으로 이동하고, 저장된 장면을 하나씩 넘기며 상영하듯 감상합니다.
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            {["필름 선택", "영사기 재생", "파노라마 감상"].map((step, index) => (
              <motion.div
                key={step}
                whileHover={{ y: -5 }}
                className="rounded-lg border border-white/10 bg-stone-950/80 p-5"
              >
                <div className="mb-4 grid h-10 w-10 place-items-center rounded-md bg-projector text-stone-950">
                  {index + 1}
                </div>
                <h3 className="font-semibold text-white">{step}</h3>
                <p className="mt-2 text-sm leading-6 text-stone-400">
                  추억을 고르고 장면을 넘기며 하나의 상영처럼 다시 봅니다.
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
