import { Suspense } from "react";
import { AuthCallbackClient } from "@/app/auth/callback/AuthCallbackClient";

export default function AuthCallbackPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-5">
      <Suspense fallback={<div className="text-stone-200">로그인 처리 중...</div>}>
        <AuthCallbackClient />
      </Suspense>
    </main>
  );
}
