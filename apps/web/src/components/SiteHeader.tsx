"use client";

import Link from "next/link";
import { type KeyboardEvent as ReactKeyboardEvent, type MouseEvent, useEffect, useState } from "react";
import { Home, LayoutDashboard, LogIn, LogOut, Menu, Projector, Sparkles, UserCog, X } from "lucide-react";
import { LoginPanel } from "@/components/LoginPanel";
import { clearAuthSession, getAccessToken, verifyAuthSession } from "@/lib/auth";
import type { MeResponse } from "@/lib/types";

export function SiteHeader() {
  const [currentUser, setCurrentUser] = useState<MeResponse | null>(null);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const isLoggedIn = Boolean(currentUser);

  useEffect(() => {
    let cancelled = false;

    const syncAuth = () => {
      const accessToken = getAccessToken();
      if (!accessToken) {
        setCurrentUser(null);
        return;
      }

      void verifyAuthSession().then((me) => {
        if (!cancelled) {
          setCurrentUser(me);
        }
      });
    };
    const openLoginEvent = () => setIsLoginOpen(true);

    syncAuth();
    window.addEventListener("storage", syncAuth);
    window.addEventListener("cinema-memory:auth-changed", syncAuth);
    window.addEventListener("cinema-memory:open-login", openLoginEvent);
    return () => {
      cancelled = true;
      window.removeEventListener("storage", syncAuth);
      window.removeEventListener("cinema-memory:auth-changed", syncAuth);
      window.removeEventListener("cinema-memory:open-login", openLoginEvent);
    };
  }, []);

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    const closeOnEscape = (event: globalThis.KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsMenuOpen(false);
      }
    };

    window.addEventListener("keydown", closeOnEscape);
    return () => {
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [isMenuOpen]);

  function logout() {
    setIsMenuOpen(false);
    clearAuthSession();
    window.location.assign("/");
  }

  function openLogin() {
    setIsMenuOpen(false);
    setIsLoginOpen(true);
  }

  function closeLogin() {
    setIsLoginOpen(false);
  }

  function closeMenu() {
    setIsMenuOpen(false);
  }

  function handleLoginOverlayKeyDown(event: ReactKeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      closeLogin();
    }
  }

  function handleLoginOverlayMouseDown(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) {
      closeLogin();
    }
  }

  function handleMenuOverlayMouseDown(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) {
      closeMenu();
    }
  }

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-white/10 bg-black/72 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-5 sm:px-8 lg:px-10">
          <Link href="/" className="flex items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded-md bg-projector text-stone-950 shadow-[0_0_34px_rgba(246,211,101,0.32)]">
              <Projector size={20} />
            </span>
            <span>
              <span className="block text-sm font-semibold uppercase tracking-[0.2em] text-projector">Cinema</span>
              <span className="block text-base font-semibold text-white">Memory</span>
            </span>
          </Link>

          <div className="flex items-center gap-2">
            {isLoggedIn ? (
              <button
                type="button"
                onClick={logout}
                className="inline-flex h-10 items-center gap-2 rounded-md border border-white/10 px-3 text-sm text-stone-200 transition hover:bg-white/10 hover:text-white"
              >
                <LogOut size={16} />
                <span className="hidden sm:inline">로그아웃</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={openLogin}
                className="inline-flex h-10 items-center gap-2 rounded-md bg-projector px-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-200"
              >
                <LogIn size={16} />
                <span className="hidden sm:inline">로그인</span>
              </button>
            )}
            <button
              type="button"
              aria-label="메뉴 열기"
              aria-controls="site-sidebar-menu"
              aria-expanded={isMenuOpen}
              onClick={() => setIsMenuOpen(true)}
              className="inline-grid h-10 w-10 place-items-center rounded-md border border-white/10 text-stone-200 transition hover:bg-white/10 hover:text-white"
            >
              <Menu size={20} />
            </button>
          </div>
        </div>
        <div className="h-px bg-gradient-to-r from-transparent via-projector/70 to-transparent" />
      </header>

      <div
        role="dialog"
        aria-modal="true"
        aria-label="사이트 메뉴"
        aria-hidden={!isMenuOpen}
        inert={!isMenuOpen}
        className={`fixed inset-0 z-[70] flex justify-end bg-transparent transition-opacity duration-300 ${
          isMenuOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0"
        }`}
        onMouseDown={handleMenuOverlayMouseDown}
      >
        <aside
          id="site-sidebar-menu"
          className={`flex h-full w-full max-w-sm flex-col border-l border-white/10 bg-stone-950/95 p-5 shadow-reel transition-transform duration-300 ease-out ${
            isMenuOpen ? "translate-x-0" : "translate-x-full"
          }`}
        >
          <div className="flex items-center justify-between gap-4">
            <Link href="/" onClick={closeMenu} className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-md bg-projector text-stone-950">
                <Projector size={20} />
              </span>
              <span>
                <span className="block text-sm font-semibold uppercase tracking-[0.2em] text-projector">Cinema</span>
                <span className="block text-base font-semibold text-white">Memory</span>
              </span>
            </Link>
            <button
              type="button"
              aria-label="메뉴 닫기"
              onClick={closeMenu}
              className="grid h-10 w-10 place-items-center rounded-md border border-white/10 text-stone-200 transition hover:bg-white/10 hover:text-white"
            >
              <X size={18} />
            </button>
          </div>

          <nav className="mt-8 grid gap-2 text-sm">
            <Link
              href="/"
              onClick={closeMenu}
              className="inline-flex items-center gap-3 rounded-md px-3 py-3 text-stone-200 transition hover:bg-white/10 hover:text-white"
            >
              <Home size={17} />
              메인
            </Link>

            {isLoggedIn ? (
              <>
                <Link
                  href="/dashboard"
                  onClick={closeMenu}
                  className="inline-flex items-center gap-3 rounded-md px-3 py-3 text-stone-200 transition hover:bg-white/10 hover:text-white"
                >
                  <LayoutDashboard size={17} />
                  대시보드
                </Link>
                {currentUser?.admin && (
                  <Link
                    href="/admin"
                    onClick={closeMenu}
                    className="inline-flex items-center gap-3 rounded-md px-3 py-3 text-stone-200 transition hover:bg-white/10 hover:text-white"
                  >
                    <UserCog size={17} />
                    Admin
                  </Link>
                )}
              </>
            ) : (
              <>
                <Link
                  href="/#projector"
                  onClick={closeMenu}
                  className="inline-flex items-center gap-3 rounded-md px-3 py-3 text-stone-200 transition hover:bg-white/10 hover:text-white"
                >
                  <Sparkles size={17} />
                  상영 경험
                </Link>
              </>
            )}
          </nav>

          <div className="mt-auto flex items-center gap-2 border-t border-white/10 pt-5 text-[10px] font-semibold uppercase tracking-[0.34em] text-projector/70">
            <Sparkles size={12} />
            Now Showing
          </div>
        </aside>
      </div>

      {isLoginOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-[80] grid place-items-center bg-black/72 px-5 backdrop-blur-sm"
          onKeyDown={handleLoginOverlayKeyDown}
          onMouseDown={handleLoginOverlayMouseDown}
        >
          <div className="relative w-full max-w-md">
            <button
              type="button"
              onClick={closeLogin}
              className="absolute -right-2 -top-12 rounded-md border border-white/10 bg-black/70 px-3 py-2 text-sm text-stone-200 transition hover:bg-white/10"
            >
              닫기
            </button>
            <LoginPanel onSuccess={closeLogin} />
          </div>
        </div>
      )}
    </>
  );
}
