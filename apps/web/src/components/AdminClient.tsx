"use client";

import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Clapperboard, Film, RefreshCw, Search, ShieldCheck, Trash2, UserCheck, Users, UserX, X } from "lucide-react";
import { AppDialog } from "@/components/AppDialog";
import {
  deleteAdminUser,
  getAdminFilms,
  getAdminSummary,
  getAdminUsers,
  isApiError,
  updateAdminUserRole,
  updateAdminUserStatus
} from "@/lib/api";
import { getAccessToken, verifyAuthSession } from "@/lib/auth";
import type { AdminFilm, AdminSummary, AdminUser, UserRole, UserStatus } from "@/lib/types";

const EMPTY_SUMMARY: AdminSummary = {
  userCount: 0,
  adminCount: 0,
  suspendedCount: 0,
  filmCount: 0,
  sceneCount: 0,
  mediaCount: 0
};

type UserRoleFilter = "ALL" | UserRole;
type UserStatusFilter = "ALL" | UserStatus;
type UserSearchField = "ALL" | "EMAIL" | "NAME";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "2-digit"
  }).format(new Date(value));
}

function errorMessage(error: unknown) {
  if (isApiError(error)) {
    if (error.status === 403) {
      return "Admin role is required.";
    }

    return `Request failed with HTTP ${error.status}.`;
  }

  return "The admin console could not be loaded.";
}

function StatTile({ label, value, icon }: { label: string; value: number; icon: ReactNode }) {
  return (
    <div className="rounded-lg border border-white/10 bg-stone-950/80 p-4">
      <div className="flex items-center justify-between gap-3 text-stone-400">
        <span className="text-xs font-semibold uppercase tracking-[0.2em]">{label}</span>
        <span className="text-projector">{icon}</span>
      </div>
      <p className="mt-4 text-3xl font-semibold text-white">{value.toLocaleString()}</p>
    </div>
  );
}

export function AdminClient() {
  const router = useRouter();
  const [isReady, setIsReady] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [currentUserEmail, setCurrentUserEmail] = useState("");
  const [summary, setSummary] = useState<AdminSummary>(EMPTY_SUMMARY);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [films, setFilms] = useState<AdminFilm[]>([]);
  const [updatingUserId, setUpdatingUserId] = useState<number | null>(null);
  const [userRoleFilter, setUserRoleFilter] = useState<UserRoleFilter>("ALL");
  const [userStatusFilter, setUserStatusFilter] = useState<UserStatusFilter>("ALL");
  const [userSearchField, setUserSearchField] = useState<UserSearchField>("ALL");
  const [userSearchQuery, setUserSearchQuery] = useState("");
  const [debouncedUserSearchQuery, setDebouncedUserSearchQuery] = useState("");
  const [deleteTargetUser, setDeleteTargetUser] = useState<AdminUser | null>(null);
  const [isDeletingUser, setIsDeletingUser] = useState(false);

  const latestFilms = useMemo(() => films.slice(0, 8), [films]);
  const filteredUsers = useMemo(() => {
    const normalizedQuery = debouncedUserSearchQuery.trim().toLowerCase();

    return users.filter((user) => {
      if (userRoleFilter !== "ALL" && user.role !== userRoleFilter) {
        return false;
      }

      if (userStatusFilter !== "ALL" && user.status !== userStatusFilter) {
        return false;
      }

      if (!normalizedQuery) {
        return true;
      }

      const searchableValues =
        userSearchField === "EMAIL"
          ? [user.email]
          : userSearchField === "NAME"
            ? [user.displayName]
            : [user.email, user.displayName, user.role, user.status];

      return searchableValues.some((value) => value.toLowerCase().includes(normalizedQuery));
    });
  }, [debouncedUserSearchQuery, userRoleFilter, userSearchField, userStatusFilter, users]);

  async function loadAdminData() {
    const token = getAccessToken();
    const [nextSummary, nextUsers, nextFilms] = await Promise.all([
      getAdminSummary(token),
      getAdminUsers(token),
      getAdminFilms(token)
    ]);

    setSummary(nextSummary);
    setUsers(nextUsers);
    setFilms(nextFilms);
  }

  useEffect(() => {
    let cancelled = false;

    void verifyAuthSession().then(async (me) => {
      if (cancelled) {
        return;
      }

      if (!me) {
        router.replace("/");
        window.dispatchEvent(new Event("cinema-memory:open-login"));
        return;
      }

      setCurrentUserEmail(me.email);

      if (!me.admin) {
        setError("Admin role is required.");
        setIsReady(true);
        return;
      }

      try {
        await loadAdminData();
      } catch (loadError) {
        if (!cancelled) {
          setError(errorMessage(loadError));
        }
      } finally {
        if (!cancelled) {
          setIsReady(true);
        }
      }
    });

    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      setDebouncedUserSearchQuery(userSearchQuery);
    }, 300);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [userSearchQuery]);

  async function refresh() {
    setIsRefreshing(true);
    setError("");

    try {
      await loadAdminData();
    } catch (refreshError) {
      setError(errorMessage(refreshError));
    } finally {
      setIsRefreshing(false);
    }
  }

  async function changeRole(user: AdminUser, role: UserRole) {
    if (user.role === role || user.email === currentUserEmail) {
      return;
    }

    setUpdatingUserId(user.id);
    setError("");

    try {
      await updateAdminUserRole(getAccessToken(), user.id, role);
      await loadAdminData();
    } catch (roleError) {
      setError(errorMessage(roleError));
    } finally {
      setUpdatingUserId(null);
    }
  }

  async function changeStatus(user: AdminUser) {
    if (user.email === currentUserEmail) {
      return;
    }

    const nextStatus: UserStatus = user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
    setUpdatingUserId(user.id);
    setError("");

    try {
      await updateAdminUserStatus(getAccessToken(), user.id, nextStatus);
      await loadAdminData();
    } catch (statusError) {
      setError(errorMessage(statusError));
    } finally {
      setUpdatingUserId(null);
    }
  }

  function removeUserAccount(user: AdminUser) {
    if (user.email === currentUserEmail) {
      return;
    }

    setDeleteTargetUser(user);
  }

  async function confirmRemoveUserAccount() {
    if (!deleteTargetUser) {
      return;
    }

    setIsDeletingUser(true);
    setUpdatingUserId(deleteTargetUser.id);
    setError("");

    try {
      await deleteAdminUser(getAccessToken(), deleteTargetUser.id);
      await loadAdminData();
      setDeleteTargetUser(null);
    } catch (deleteError) {
      setError(errorMessage(deleteError));
    } finally {
      setIsDeletingUser(false);
      setUpdatingUserId(null);
    }
  }

  if (!isReady) {
    return (
      <main className="grid min-h-[60vh] place-items-center px-5">
        <div className="rounded-lg border border-white/10 bg-black/45 px-5 py-4 text-sm text-stone-300">
          Loading admin console...
        </div>
      </main>
    );
  }

  if (error && users.length === 0 && films.length === 0) {
    return (
      <main className="min-h-screen px-5 py-12 sm:px-8 lg:px-10">
        <div className="mx-auto max-w-3xl rounded-lg border border-red-400/30 bg-red-950/20 p-6">
          <div className="flex items-center gap-3 text-red-200">
            <ShieldCheck size={20} />
            <h1 className="text-xl font-semibold">Admin access denied</h1>
          </div>
          <p className="mt-3 text-sm text-red-100/80">{error}</p>
        </div>
      </main>
    );
  }

  return (
    <>
    <main className="min-h-screen px-5 py-10 sm:px-8 lg:px-10">
      <div className="mx-auto max-w-7xl">
        <section className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-white/10 pb-7">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-md bg-projector/10 px-3 py-1 text-xs font-semibold text-projector">
              <ShieldCheck size={14} />
              Admin Console
            </div>
            <h1 className="text-3xl font-semibold text-white sm:text-4xl">Service Administration</h1>
            <p className="mt-3 max-w-2xl text-stone-300">
              Review platform activity and manage user roles from one protected workspace.
            </p>
          </div>

          <button
            type="button"
            onClick={refresh}
            disabled={isRefreshing}
            className="inline-flex h-10 items-center gap-2 rounded-md border border-white/10 px-3 text-sm text-stone-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw size={16} className={isRefreshing ? "animate-spin" : ""} />
            Refresh
          </button>
        </section>

        {error && (
          <div className="mb-6 rounded-lg border border-red-400/30 bg-red-950/20 px-4 py-3 text-sm text-red-100">
            {error}
          </div>
        )}

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
          <StatTile label="Regular Users" value={summary.userCount} icon={<Users size={18} />} />
          <StatTile label="Admins" value={summary.adminCount} icon={<ShieldCheck size={18} />} />
          <StatTile label="Suspended" value={summary.suspendedCount} icon={<UserX size={18} />} />
          <StatTile label="Films" value={summary.filmCount} icon={<Film size={18} />} />
          <StatTile label="Scenes" value={summary.sceneCount} icon={<Clapperboard size={18} />} />
          <StatTile label="Media" value={summary.mediaCount} icon={<Film size={18} />} />
        </section>

        <section className="mt-8 grid gap-8 xl:grid-cols-[minmax(0,1.2fr)_minmax(24rem,0.8fr)]">
          <div>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-white">Users</h2>
              <span className="text-sm text-stone-500">
                {filteredUsers.length.toLocaleString()} / {users.length.toLocaleString()} shown
              </span>
            </div>

            <div className="mb-4 grid gap-3 rounded-lg border border-white/10 bg-stone-950/80 p-4 xl:grid-cols-[auto_auto_minmax(0,1fr)]">
              <div className="flex flex-wrap gap-2">
                {(["ALL", "USER", "ADMIN"] as const).map((role) => (
                  <button
                    key={role}
                    type="button"
                    onClick={() => setUserRoleFilter(role)}
                    className={`h-10 rounded-md px-3 text-sm font-semibold transition ${
                      userRoleFilter === role
                        ? "bg-projector text-stone-950"
                        : "border border-white/10 text-stone-200 hover:bg-white/10"
                    }`}
                  >
                    {role === "ALL" ? "All" : role === "USER" ? "Users" : "Admins"}
                  </button>
                ))}
              </div>

              <div className="flex flex-wrap gap-2">
                {(["ALL", "ACTIVE", "SUSPENDED"] as const).map((status) => (
                  <button
                    key={status}
                    type="button"
                    onClick={() => setUserStatusFilter(status)}
                    className={`h-10 rounded-md px-3 text-sm font-semibold transition ${
                      userStatusFilter === status
                        ? "bg-projector text-stone-950"
                        : "border border-white/10 text-stone-200 hover:bg-white/10"
                    }`}
                  >
                    {status === "ALL" ? "Any status" : status === "ACTIVE" ? "Active" : "Suspended"}
                  </button>
                ))}
              </div>

              <div className="grid gap-2 sm:grid-cols-[9rem_minmax(0,1fr)]">
                <select
                  value={userSearchField}
                  onChange={(event) => setUserSearchField(event.target.value as UserSearchField)}
                  className="h-10 rounded-md border border-white/10 bg-black/45 px-3 text-sm text-white outline-none transition focus:border-projector"
                  aria-label="User search category"
                >
                  <option value="ALL">All fields</option>
                  <option value="EMAIL">Email</option>
                  <option value="NAME">Name</option>
                </select>

                <div className="relative">
                  <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-stone-500" />
                  <input
                    value={userSearchQuery}
                    onChange={(event) => setUserSearchQuery(event.target.value)}
                    className="h-10 w-full rounded-md border border-white/10 bg-black/45 pl-10 pr-10 text-sm text-white outline-none transition placeholder:text-stone-600 focus:border-projector"
                    placeholder="Search users"
                  />
                  {userSearchQuery && (
                    <button
                      type="button"
                      aria-label="Clear user search"
                      onClick={() => setUserSearchQuery("")}
                      className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center rounded-md text-stone-400 transition hover:bg-white/10 hover:text-white"
                    >
                      <X size={15} />
                    </button>
                  )}
                </div>
              </div>
            </div>

            <div className="overflow-x-auto rounded-lg border border-white/10 bg-stone-950/80">
              <table className="min-w-full divide-y divide-white/10 text-left text-sm">
                <thead className="bg-white/[0.03] text-xs uppercase tracking-[0.16em] text-stone-500">
                  <tr>
                    <th className="px-4 py-3 font-semibold">User</th>
                    <th className="px-4 py-3 font-semibold">Role</th>
                    <th className="px-4 py-3 font-semibold">Status</th>
                    <th className="px-4 py-3 font-semibold">Joined</th>
                    <th className="px-4 py-3 font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {filteredUsers.map((user) => {
                    const isSelf = user.email === currentUserEmail;
                    const nextRole: UserRole = user.role === "ADMIN" ? "USER" : "ADMIN";

                    return (
                      <tr key={user.id} className="align-top">
                        <td className="px-4 py-4">
                          <p className="font-medium text-white">{user.displayName}</p>
                          <p className="mt-1 text-xs text-stone-500">{user.email}</p>
                        </td>
                        <td className="px-4 py-4">
                          <span
                            className={`inline-flex rounded-md px-2 py-1 text-xs font-semibold ${
                              user.role === "ADMIN"
                                ? "bg-projector/15 text-projector"
                                : "bg-white/10 text-stone-300"
                            }`}
                          >
                            {user.role}
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          <span
                            className={`inline-flex rounded-md px-2 py-1 text-xs font-semibold ${
                              user.status === "ACTIVE"
                                ? "bg-emerald-400/15 text-emerald-200"
                                : "bg-red-400/15 text-red-200"
                            }`}
                          >
                            {user.status}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-stone-400">{formatDate(user.createdAt)}</td>
                        <td className="px-4 py-4">
                          <div className="flex flex-wrap gap-2">
                            <button
                              type="button"
                              disabled={isSelf || updatingUserId === user.id}
                              onClick={() => changeRole(user, nextRole)}
                              className="inline-flex h-9 items-center gap-1.5 rounded-md border border-white/10 px-3 text-xs font-semibold text-stone-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              <ShieldCheck size={13} />
                              {isSelf ? "Current" : user.role === "ADMIN" ? "Set user" : "Set admin"}
                            </button>
                            <button
                              type="button"
                              disabled={isSelf || updatingUserId === user.id}
                              onClick={() => changeStatus(user)}
                              className={`inline-flex h-9 items-center gap-1.5 rounded-md border px-3 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${
                                user.status === "ACTIVE"
                                  ? "border-amber-300/30 text-amber-200 hover:bg-amber-950/30"
                                  : "border-emerald-300/30 text-emerald-200 hover:bg-emerald-950/30"
                              }`}
                            >
                              {user.status === "ACTIVE" ? <UserX size={13} /> : <UserCheck size={13} />}
                              {user.status === "ACTIVE" ? "Suspend" : "Reactivate"}
                            </button>
                            <button
                              type="button"
                              disabled={isSelf || updatingUserId === user.id}
                              onClick={() => removeUserAccount(user)}
                              className="inline-flex h-9 items-center gap-1.5 rounded-md border border-red-400/30 px-3 text-xs font-semibold text-red-200 transition hover:bg-red-950/30 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              <Trash2 size={13} />
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {filteredUsers.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-4 py-8 text-center text-sm text-stone-500">
                        No users match the current filters.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-white">Recent Films</h2>
              <span className="text-sm text-stone-500">{films.length.toLocaleString()} total</span>
            </div>

            <div className="grid gap-3">
              {latestFilms.length === 0 ? (
                <div className="rounded-lg border border-white/10 bg-stone-950/80 p-5 text-sm text-stone-400">
                  No films have been created yet.
                </div>
              ) : (
                latestFilms.map((film) => (
                  <article key={film.id} className="rounded-lg border border-white/10 bg-stone-950/80 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="truncate font-semibold text-white">{film.title}</h3>
                        <p className="mt-1 text-xs text-stone-500">
                          {film.ownerDisplayName} / {film.ownerEmail}
                        </p>
                      </div>
                      <span className="rounded-md bg-white/10 px-2 py-1 text-xs text-stone-300">
                        {film.visibility}
                      </span>
                    </div>
                    <div className="mt-4 flex items-center justify-between text-xs text-stone-500">
                      <span>{film.sceneCount} scenes</span>
                      <span>{formatDate(film.createdAt)}</span>
                    </div>
                  </article>
                ))
              )}
            </div>
          </div>
        </section>
      </div>
    </main>

    <AppDialog
      open={Boolean(deleteTargetUser)}
      title="계정을 삭제할까요?"
      description={
        <>
          <span className="font-semibold text-white">{deleteTargetUser?.email}</span> 계정과 이 사용자의 필름, 장면, 미디어 데이터가 함께 삭제됩니다.
        </>
      }
      confirmLabel={isDeletingUser ? "삭제 중..." : "삭제"}
      cancelLabel="취소"
      tone="danger"
      isBusy={isDeletingUser}
      onConfirm={() => void confirmRemoveUserAccount()}
      onClose={() => setDeleteTargetUser(null)}
    />
    </>
  );
}
