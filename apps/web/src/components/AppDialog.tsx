"use client";

import { type KeyboardEvent, type ReactNode } from "react";
import { AlertTriangle, Info, X } from "lucide-react";

type DialogTone = "default" | "danger";

type Props = {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: DialogTone;
  isBusy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

export function AppDialog({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel,
  tone = "default",
  isBusy = false,
  onConfirm,
  onClose
}: Props) {
  if (!open) {
    return null;
  }

  const isDanger = tone === "danger";

  function onOverlayKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape" && !isBusy) {
      onClose();
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="app-dialog-title"
      className="fixed inset-0 z-[100] grid place-items-center bg-black/72 px-5 backdrop-blur-sm"
      onKeyDown={onOverlayKeyDown}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isBusy) {
          onClose();
        }
      }}
    >
      <div className="relative w-full max-w-md rounded-lg border border-white/10 bg-stone-950 p-5 shadow-reel">
        <button
          type="button"
          aria-label="닫기"
          disabled={isBusy}
          onClick={onClose}
          className="absolute right-3 top-3 grid h-8 w-8 place-items-center rounded-md text-stone-400 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
        >
          <X size={17} />
        </button>

        <div
          className={`grid h-11 w-11 place-items-center rounded-md ${
            isDanger ? "bg-red-400/15 text-red-200" : "bg-projector/15 text-projector"
          }`}
        >
          {isDanger ? <AlertTriangle size={21} /> : <Info size={21} />}
        </div>

        <h2 id="app-dialog-title" className="mt-4 pr-8 text-xl font-semibold text-white">
          {title}
        </h2>
        <div className="mt-3 text-sm leading-6 text-stone-300">{description}</div>

        <div className="mt-6 flex justify-end gap-2">
          {cancelLabel && (
            <button
              type="button"
              disabled={isBusy}
              onClick={onClose}
              className="inline-flex h-10 items-center rounded-md border border-white/10 px-4 text-sm font-semibold text-stone-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {cancelLabel}
            </button>
          )}
          <button
            type="button"
            disabled={isBusy}
            onClick={onConfirm}
            className={`inline-flex h-10 items-center rounded-md px-4 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${
              isDanger
                ? "bg-red-300 text-red-950 hover:bg-red-200"
                : "bg-projector text-stone-950 hover:bg-amber-200"
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
