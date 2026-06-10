"use client";

import { type FormEvent, type KeyboardEvent, useState } from "react";
import { KeyRound, Loader2, Mail, UserRound } from "lucide-react";
import { isApiError, login, signup } from "@/lib/api";
import { persistAuthSession } from "@/lib/auth";
import { OAuthButtons } from "@/components/OAuthButtons";
import type { AuthTokens } from "@/lib/types";

type Mode = "login" | "signup";

type Props = {
  onSuccess?: () => void;
};

type LoginErrors = {
  email?: string;
  password?: string;
  displayName?: string;
};

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function fieldClass(hasError: boolean) {
  return `flex items-center gap-2 rounded-md border bg-black/45 px-3 py-2 transition focus-within:border-projector ${
    hasError ? "border-red-400/80" : "border-white/10"
  }`;
}

function dashboardRedirect() {
  window.location.assign("/dashboard");
}

export function LoginPanel({ onSuccess }: Props) {
  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [message, setMessage] = useState("백엔드가 실행 중이면 이메일 로그인과 회원가입을 바로 테스트할 수 있습니다.");
  const [errors, setErrors] = useState<LoginErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate() {
    const nextErrors: LoginErrors = {};

    if (!email.trim()) {
      nextErrors.email = "이메일을 입력해주세요.";
    } else if (!isValidEmail(email.trim())) {
      nextErrors.email = "올바른 이메일 형식이 아닙니다.";
    }

    if (!password) {
      nextErrors.password = "비밀번호를 입력해주세요.";
    } else if (password.length < 8) {
      nextErrors.password = "비밀번호는 8자 이상이어야 합니다.";
    }

    if (mode === "signup") {
      if (!displayName.trim()) {
        nextErrors.displayName = "닉네임을 입력해주세요.";
      } else if (displayName.trim().length < 2) {
        nextErrors.displayName = "닉네임은 2자 이상이어야 합니다.";
      }
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function requestTokens(normalizedEmail: string): Promise<AuthTokens> {
    if (mode === "login") {
      return login({ email: normalizedEmail, password });
    }

    try {
      return await signup({ email: normalizedEmail, password, displayName: displayName.trim() });
    } catch (error) {
      if (isApiError(error) && error.status === 409) {
        setMessage("이미 가입된 이메일입니다. 같은 비밀번호로 로그인합니다.");
        return login({ email: normalizedEmail, password });
      }

      throw error;
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    if (!validate()) {
      setMessage("입력값을 다시 확인해주세요.");
      return;
    }

    const normalizedEmail = email.trim().toLowerCase();
    setIsSubmitting(true);
    setMessage("인증 서버에 요청 중입니다...");

    let tokens: AuthTokens;
    try {
      tokens = await requestTokens(normalizedEmail);
    } catch (error) {
      const invalidCredentials = isApiError(error) && error.status === 401;
      const duplicatedSignup = mode === "signup" && isApiError(error) && error.status === 409;

      setErrors({
        email: invalidCredentials || duplicatedSignup ? "이메일 또는 비밀번호가 올바르지 않습니다." : "인증 요청에 실패했습니다.",
        password: invalidCredentials || duplicatedSignup ? "이메일 또는 비밀번호가 올바르지 않습니다." : undefined
      });
      setMessage(
        duplicatedSignup
          ? "이미 가입된 이메일입니다. 기존 비밀번호로 로그인하세요."
          : "로그인 요청에 실패했습니다. 백엔드 실행 상태와 계정 정보를 확인하세요."
      );
      setIsSubmitting(false);
      return;
    }

    try {
      persistAuthSession(normalizedEmail, tokens);
    } catch {
      setErrors({ email: "브라우저 저장소에 토큰을 저장하지 못했습니다." });
      setMessage("로그인은 성공했지만 브라우저 저장소에 토큰을 저장하지 못했습니다.");
      setIsSubmitting(false);
      return;
    }

    setMessage("로그인 성공. 필름 보관함으로 이동합니다.");
    onSuccess?.();
    dashboardRedirect();
  }

  function onFormKeyDown(event: KeyboardEvent<HTMLFormElement>) {
    if (event.key !== "Enter" || event.nativeEvent.isComposing) {
      return;
    }

    if (event.target instanceof HTMLInputElement) {
      event.preventDefault();
      event.currentTarget.requestSubmit();
    }
  }

  return (
    <section className="relative overflow-hidden rounded-lg border border-white/10 bg-stone-950/95 p-5 shadow-reel backdrop-blur md:p-6">
      <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-velvet via-projector to-sky-300" />
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-projector">Ticket Booth</p>
          <h2 className="mt-2 text-2xl font-semibold text-white">이메일로 입장하기</h2>
        </div>
        <div className="rounded-md bg-white/10 p-3 text-projector">
          <KeyRound size={22} />
        </div>
      </div>

      <div className="mb-5 grid grid-cols-2 rounded-md border border-white/10 bg-black/35 p-1">
        {(["login", "signup"] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => {
              setMode(item);
              setErrors({});
              setMessage(item === "login" ? "이메일로 로그인합니다." : "새 계정을 만들고 바로 로그인합니다.");
            }}
            className={`rounded px-3 py-2 text-sm font-semibold transition ${
              mode === item ? "bg-projector text-stone-950" : "text-stone-300 hover:text-white"
            }`}
          >
            {item === "login" ? "로그인" : "회원가입"}
          </button>
        ))}
      </div>

      <form className="grid gap-4" onSubmit={onSubmit} onKeyDown={onFormKeyDown} noValidate>
        {mode === "signup" && (
          <label className="grid gap-2 text-sm text-stone-300">
            닉네임
            <span className={fieldClass(Boolean(errors.displayName))}>
              <UserRound size={16} className="text-stone-500" />
              <input
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                className="w-full bg-transparent text-white outline-none"
                placeholder="필름에 표시할 이름"
              />
            </span>
            {errors.displayName && <span className="text-xs font-medium text-red-400">{errors.displayName}</span>}
          </label>
        )}

        <label className="grid gap-2 text-sm text-stone-300">
          이메일
          <span className={fieldClass(Boolean(errors.email))}>
            <Mail size={16} className="text-stone-500" />
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full bg-transparent text-white outline-none"
              placeholder="you@example.com"
              autoFocus
            />
          </span>
          {errors.email && <span className="text-xs font-medium text-red-400">{errors.email}</span>}
        </label>

        <label className="grid gap-2 text-sm text-stone-300">
          비밀번호
          <span className={fieldClass(Boolean(errors.password))}>
            <KeyRound size={16} className="text-stone-500" />
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full bg-transparent text-white outline-none"
              placeholder="8자 이상"
            />
          </span>
          {errors.password && <span className="text-xs font-medium text-red-400">{errors.password}</span>}
        </label>

        <button
          type="submit"
          disabled={isSubmitting}
          className="inline-flex items-center justify-center gap-2 rounded-md bg-projector px-4 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-200 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {isSubmitting && <Loader2 size={16} className="animate-spin" />}
          {mode === "login" ? "로그인" : "회원가입 후 로그인"}
        </button>
      </form>

      <div className="mt-5 rounded-md border border-white/10 bg-black/30 p-3 text-sm leading-6 text-stone-300">
        {message}
      </div>

      <div className="mt-4 border-t border-white/10 pt-4">
        <div className="mb-3 flex items-center gap-3">
          <span className="h-px flex-1 bg-white/10" />
          <span className="text-xs font-semibold text-stone-500">소셜 계정으로 계속하기</span>
          <span className="h-px flex-1 bg-white/10" />
        </div>
        <OAuthButtons />
      </div>
    </section>
  );
}
