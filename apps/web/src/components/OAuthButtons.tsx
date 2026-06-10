"use client";

import { Film, KeyRound } from "lucide-react";
import { oauthUrl } from "@/lib/api";

export function OAuthButtons() {
  return (
    <div className="flex flex-wrap gap-3">
      <a
        href={oauthUrl("google")}
        className="inline-flex items-center gap-2 rounded-md bg-stone-50 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-projector"
      >
        <KeyRound size={16} />
        Google로 로그인
      </a>
      <a
        href={oauthUrl("kakao")}
        className="inline-flex items-center gap-2 rounded-md bg-[#fee500] px-4 py-2 text-sm font-semibold text-stone-950 transition hover:brightness-95"
      >
        <Film size={16} />
        Kakao로 로그인
      </a>
    </div>
  );
}
