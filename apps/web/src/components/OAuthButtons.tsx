"use client";

import { ArrowRight, MessageCircle } from "lucide-react";
import { oauthUrl } from "@/lib/api";

const providers = [
  {
    id: "google" as const,
    label: "Google 로그인",
    mark: <span className="text-[17px] font-bold leading-none text-[#4285f4]">G</span>,
    className: "border-white/12 bg-white text-stone-950 hover:bg-stone-100"
  },
  {
    id: "kakao" as const,
    label: "Kakao 로그인",
    mark: <MessageCircle size={18} strokeWidth={2.6} className="fill-stone-950 text-stone-950" />,
    className: "border-[#fee500] bg-[#fee500] text-[#191600] hover:bg-[#f4da00]"
  }
];

export function OAuthButtons() {
  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {providers.map((provider) => (
        <a
          key={provider.id}
          href={oauthUrl(provider.id)}
          className={`group inline-flex h-12 items-center justify-between rounded-md border px-3 text-sm font-semibold shadow-[0_10px_30px_rgba(0,0,0,0.18)] transition hover:-translate-y-0.5 focus:outline-none focus:ring-2 focus:ring-projector/70 ${provider.className}`}
        >
          <span className="inline-flex min-w-0 items-center gap-3">
            <span className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-white shadow-[inset_0_0_0_1px_rgba(0,0,0,0.08)]">
              {provider.mark}
            </span>
            <span className="truncate">{provider.label}</span>
          </span>
          <ArrowRight size={16} className="shrink-0 opacity-45 transition group-hover:translate-x-0.5 group-hover:opacity-80" />
        </a>
      ))}
    </div>
  );
}
