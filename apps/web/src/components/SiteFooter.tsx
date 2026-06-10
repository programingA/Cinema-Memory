import { Film, Github, Projector } from "lucide-react";
import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-black/72">
      <div className="mx-auto grid max-w-7xl gap-8 px-5 py-10 text-sm text-stone-300 sm:px-8 lg:grid-cols-[1.3fr_1fr_1fr] lg:px-10">
        <div>
          <div className="mb-3 flex items-center gap-3 text-white">
            <span className="grid h-9 w-9 place-items-center rounded-md bg-white/10">
              <Projector size={18} />
            </span>
            <span className="font-semibold">Cinema Memory</span>
          </div>
          <p className="max-w-md leading-6">
            필름처럼 추억을 모으고, 영사기처럼 다시 재생하는 개인 기록 웹 서비스입니다.
          </p>
        </div>

        <div>
          <h2 className="mb-3 text-sm font-semibold text-white">메뉴</h2>
          <div className="grid gap-2">
            <Link href="/#films" className="hover:text-white">필름 보관함</Link>
            <Link href="/#projector" className="hover:text-white">상영 경험</Link>
            <Link href="/dashboard" className="hover:text-white">대시보드</Link>
          </div>
        </div>

        <div>
          <h2 className="mb-3 text-sm font-semibold text-white">개발 상태</h2>
          <div className="grid gap-2">
            <span className="inline-flex items-center gap-2"><Film size={14} /> MVP UI 구현</span>
            <span className="inline-flex items-center gap-2"><Github size={14} /> OAuth는 키 발급 후 연결</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
