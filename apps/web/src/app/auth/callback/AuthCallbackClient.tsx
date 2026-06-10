"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { persistVerifiedAuthSession } from "@/lib/auth";

export function AuthCallbackClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const accessToken = searchParams.get("accessToken");
  const refreshToken = searchParams.get("refreshToken");
  const hasTokens = useMemo(() => Boolean(accessToken && refreshToken), [accessToken, refreshToken]);
  const [message, setMessage] = useState("OAuth 로그인 토큰을 확인하고 있습니다.");

  useEffect(() => {
    if (!accessToken || !refreshToken) {
      queueMicrotask(() => setMessage("OAuth 로그인 토큰이 없습니다."));
      return;
    }

    void persistVerifiedAuthSession({ accessToken, refreshToken, tokenType: "Bearer" })
      .then(() => {
        window.history.replaceState(null, "", "/auth/callback");
        router.replace("/dashboard");
      })
      .catch(() => {
        setMessage("OAuth 토큰 검증에 실패했습니다. 다시 로그인해주세요.");
      });
  }, [accessToken, refreshToken, router]);

  return (
    <section className="w-full max-w-md rounded-lg border border-white/10 bg-black/50 p-6 shadow-reel">
      <h1 className="text-2xl font-semibold text-white">
        {hasTokens ? "로그인 처리 중" : "로그인 토큰이 없습니다"}
      </h1>
      <p className="mt-3 text-sm leading-6 text-stone-300">{message}</p>
      <Link
        href="/"
        className="mt-6 inline-flex rounded-md bg-projector px-4 py-2 text-sm font-semibold text-stone-950"
      >
        홈으로 이동
      </Link>
    </section>
  );
}
