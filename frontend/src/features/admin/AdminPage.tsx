import { useEffect, useState } from "react";
import { Users, CheckCircle2, XCircle, RefreshCw, LogOut, Search, Trash2, RotateCcw, Power } from "lucide-react";
import { api, post, errorText } from "../../lib/api";

interface AdminUser {
  id: number;
  kakaoId: string;
  nickname: string;
  cafeteriaName: string;
  cotNo: string;
  hallNo: string;
  scheduledDays: string;
  scheduledTime: string;
  isEnabled: boolean;
  lastSentDate: string | null;
}

interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  sentTodayCount: number;
  cafeteriaDistribution: Record<string, number>;
}

const DAY_NAMES = ["", "월", "화", "수", "목", "금", "토", "일"];

function formatDays(daysStr: string): string {
  if (!daysStr) return "-";
  return daysStr
    .split(",")
    .map((d) => DAY_NAMES[Number(d)] || d)
    .join(", ");
}

export const AdminPage = (): React.JSX.Element => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const checkAuth = async () => {
    try {
      const res = await api<{ authenticated: boolean }>("/admin/check");
      setIsAuthenticated(res.authenticated);
      if (res.authenticated) {
        loadData();
      }
    } catch {
      setIsAuthenticated(false);
    }
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [userList, statsData] = await Promise.all([
        api<AdminUser[]>("/admin/users"),
        api<AdminStats>("/admin/stats"),
      ]);
      setUsers(userList);
      setStats(statsData);
    } catch (e) {
      setNotice(errorText(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) return;
    setLoginLoading(true);
    setLoginError("");
    try {
      await post("/admin/login", { secretKey: password.trim() });
      setIsAuthenticated(true);
      setPassword("");
      loadData();
    } catch (err) {
      setLoginError(errorText(err));
    } finally {
      setLoginLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await post("/admin/logout", {});
    } finally {
      setIsAuthenticated(false);
      setUsers([]);
      setStats(null);
    }
  };

  const handleToggle = async (user: AdminUser) => {
    setActionLoadingId(user.kakaoId);
    setNotice(null);
    try {
      await post(`/admin/users/${user.kakaoId}/toggle`, { isEnabled: !user.isEnabled });
      setUsers((prev) =>
        prev.map((u) => (u.kakaoId === user.kakaoId ? { ...u, isEnabled: !u.isEnabled } : u))
      );
      if (stats) {
        setStats({
          ...stats,
          activeUsers: stats.activeUsers + (user.isEnabled ? -1 : 1),
          inactiveUsers: stats.inactiveUsers + (user.isEnabled ? 1 : -1),
        });
      }
      setNotice(`${user.nickname} 님의 알림을 ${user.isEnabled ? "끄기" : "켜기"} 완료했습니다.`);
    } catch (e) {
      setNotice(errorText(e));
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleResetSend = async (user: AdminUser) => {
    setActionLoadingId(user.kakaoId);
    setNotice(null);
    try {
      await post(`/admin/users/${user.kakaoId}/reset-send`, {});
      setUsers((prev) =>
        prev.map((u) => (u.kakaoId === user.kakaoId ? { ...u, lastSentDate: null } : u))
      );
      setNotice(`${user.nickname} 님의 오늘 발송 이력을 초기화했습니다. (테스트 발송 가능)`);
    } catch (e) {
      setNotice(errorText(e));
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDelete = async (user: AdminUser) => {
    if (!window.confirm(`${user.nickname} (${user.kakaoId}) 님의 연동 설정을 정말 삭제하시겠습니까?`)) {
      return;
    }
    setActionLoadingId(user.kakaoId);
    setNotice(null);
    try {
      await api(`/admin/users/${user.kakaoId}`, { method: "DELETE" });
      setUsers((prev) => prev.filter((u) => u.kakaoId !== user.kakaoId));
      loadData();
      setNotice(`${user.nickname} 님의 연동 설정을 삭제했습니다.`);
    } catch (e) {
      setNotice(errorText(e));
    } finally {
      setActionLoadingId(null);
    }
  };

  if (isAuthenticated === null) {
    return (
      <section className="page-enter">
        <div style={{ textAlign: "center", padding: "60px 0", color: "var(--sub)" }}>
          관리자 상태를 확인하고 있습니다…
        </div>
      </section>
    );
  }

  if (!isAuthenticated) {
    return (
      <section className="page-enter">
        <div className="section-heading">
          <div>
            <span className="eyebrow">ADMINISTRATION</span>
            <h1>관리자 로그인</h1>
            <p>사용자 연동 및 알림 구독 현황을 관리하려면 비밀번호를 입력해 주세요.</p>
          </div>
          <Users size={42} strokeWidth={1} />
        </div>
        <div className="settings-layout" style={{ maxWidth: 460, margin: "0 auto" }}>
          <form className="settings-card" onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <label className="field-label">
              관리자 비밀번호
              <input
                type="password"
                placeholder="비밀번호 입력"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoFocus
                style={{
                  width: "100%",
                  height: 48,
                  padding: "0 14px",
                  borderRadius: 12,
                  border: "1px solid var(--line)",
                  background: "#fff",
                  marginTop: 6,
                }}
              />
            </label>
            {loginError && (
              <div style={{ color: "#d9383a", fontSize: 14 }}>{loginError}</div>
            )}
            <button
              type="submit"
              className="button active"
              disabled={loginLoading}
              style={{
                height: 48,
                background: "var(--ink)",
                color: "#fff",
                borderRadius: 12,
                fontWeight: 600,
              }}
            >
              {loginLoading ? "확인 중…" : "로그인"}
            </button>
          </form>
        </div>
      </section>
    );
  }

  const filteredUsers = users.filter((u) => {
    const matchesSearch =
      u.nickname.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.kakaoId.includes(searchTerm) ||
      u.cafeteriaName.toLowerCase().includes(searchTerm.toLowerCase());
    if (statusFilter === "ACTIVE") return matchesSearch && u.isEnabled;
    if (statusFilter === "INACTIVE") return matchesSearch && !u.isEnabled;
    return matchesSearch;
  });

  return (
    <section className="page-enter">
      <div className="section-heading" style={{ alignItems: "flex-start" }}>
        <div>
          <span className="eyebrow">ADMIN CONSOLE</span>
          <h1>연동 사용자 관리</h1>
          <p>카카오톡 식단 알림을 구독 중인 사용자 현황과 발송 상태를 관리합니다.</p>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button
            onClick={loadData}
            disabled={loading}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 6,
              padding: "10px 14px",
              borderRadius: 10,
              border: "1px solid var(--line)",
              background: "#fff",
              fontSize: 14,
            }}
          >
            <RefreshCw size={15} className={loading ? "spin" : ""} />
            새로고침
          </button>
          <button
            onClick={handleLogout}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 6,
              padding: "10px 14px",
              borderRadius: 10,
              border: "1px solid var(--line)",
              background: "#fff",
              fontSize: 14,
              color: "var(--sub)",
            }}
          >
            <LogOut size={15} />
            로그아웃
          </button>
        </div>
      </div>

      {notice && (
        <div
          style={{
            padding: "12px 16px",
            background: "#edf4ff",
            color: "#1d4ed8",
            borderRadius: 10,
            marginBottom: 20,
            fontSize: 14,
          }}
        >
          {notice}
        </div>
      )}

      {stats && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
            gap: 16,
            marginBottom: 24,
          }}
        >
          <div style={{ background: "#fff", padding: "18px 20px", borderRadius: 14, border: "1px solid var(--line)" }}>
            <div style={{ fontSize: 13, color: "var(--sub)", marginBottom: 6 }}>총 연동 계정</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{stats.totalUsers}명</div>
          </div>
          <div style={{ background: "#fff", padding: "18px 20px", borderRadius: 14, border: "1px solid var(--line)" }}>
            <div style={{ fontSize: 13, color: "#16a34a", marginBottom: 6 }}>알림 수신 중 (활성)</div>
            <div style={{ fontSize: 28, fontWeight: 700, color: "#16a34a" }}>{stats.activeUsers}명</div>
          </div>
          <div style={{ background: "#fff", padding: "18px 20px", borderRadius: 14, border: "1px solid var(--line)" }}>
            <div style={{ fontSize: 13, color: "#9ca3af", marginBottom: 6 }}>알림 꺼짐 (비활성)</div>
            <div style={{ fontSize: 28, fontWeight: 700, color: "#6b7280" }}>{stats.inactiveUsers}명</div>
          </div>
          <div style={{ background: "#fff", padding: "18px 20px", borderRadius: 14, border: "1px solid var(--line)" }}>
            <div style={{ fontSize: 13, color: "var(--blue)", marginBottom: 6 }}>오늘 발송 완료</div>
            <div style={{ fontSize: 28, fontWeight: 700, color: "var(--blue)" }}>{stats.sentTodayCount}명</div>
          </div>
        </div>
      )}

      <div
        style={{
          background: "#fff",
          borderRadius: 14,
          border: "1px solid var(--line)",
          padding: 20,
        }}
      >
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 12,
            marginBottom: 16,
          }}
        >
          <div style={{ position: "relative", minWidth: 260 }}>
            <Search size={16} style={{ position: "absolute", left: 12, top: 12, color: "var(--sub)" }} />
            <input
              type="text"
              placeholder="닉네임, 카카오 ID, 식당 검색"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                width: "100%",
                height: 40,
                padding: "0 12px 0 36px",
                borderRadius: 8,
                border: "1px solid var(--line)",
                fontSize: 14,
              }}
            />
          </div>

          <div style={{ display: "flex", gap: 8 }}>
            {(["ALL", "ACTIVE", "INACTIVE"] as const).map((filter) => (
              <button
                key={filter}
                onClick={() => setStatusFilter(filter)}
                style={{
                  padding: "8px 14px",
                  borderRadius: 8,
                  fontSize: 13,
                  fontWeight: statusFilter === filter ? 600 : 400,
                  background: statusFilter === filter ? "var(--ink)" : "var(--bg)",
                  color: statusFilter === filter ? "#fff" : "var(--sub)",
                }}
              >
                {filter === "ALL" && "전체"}
                {filter === "ACTIVE" && "활성만"}
                {filter === "INACTIVE" && "비활성만"}
              </button>
            ))}
          </div>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14, textAlign: "left" }}>
            <thead>
              <tr style={{ borderBottom: "2px solid var(--line)", color: "var(--sub)", fontSize: 13 }}>
                <th style={{ padding: "12px 10px" }}>닉네임</th>
                <th style={{ padding: "12px 10px" }}>카카오 식별자</th>
                <th style={{ padding: "12px 10px" }}>구독 구내식당</th>
                <th style={{ padding: "12px 10px" }}>발송 시간 / 요일</th>
                <th style={{ padding: "12px 10px" }}>상태</th>
                <th style={{ padding: "12px 10px" }}>최근 발송일</th>
                <th style={{ padding: "12px 10px", textAlign: "right" }}>관리 작업</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "40px 0", color: "var(--sub)" }}>
                    {users.length === 0 ? "등록된 연동 사용자가 없습니다." : "검색 조건에 맞는 사용자가 없습니다."}
                  </td>
                </tr>
              ) : (
                filteredUsers.map((u) => {
                  const isBusy = actionLoadingId === u.kakaoId;
                  return (
                    <tr key={u.id} style={{ borderBottom: "1px solid var(--line)" }}>
                      <td style={{ padding: "14px 10px", fontWeight: 600 }}>{u.nickname}</td>
                      <td style={{ padding: "14px 10px", color: "var(--sub)", fontSize: 13 }}>{u.kakaoId}</td>
                      <td style={{ padding: "14px 10px" }}>{u.cafeteriaName}</td>
                      <td style={{ padding: "14px 10px" }}>
                        <span style={{ fontWeight: 600 }}>{u.scheduledTime}</span>
                        <span style={{ color: "var(--sub)", fontSize: 12, marginLeft: 6 }}>
                          ({formatDays(u.scheduledDays)})
                        </span>
                      </td>
                      <td style={{ padding: "14px 10px" }}>
                        {u.isEnabled ? (
                          <span
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                              padding: "4px 8px",
                              borderRadius: 6,
                              background: "#eefbf1",
                              color: "#16a34a",
                              fontSize: 12,
                              fontWeight: 600,
                            }}
                          >
                            <CheckCircle2 size={13} />
                            수신중
                          </span>
                        ) : (
                          <span
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                              padding: "4px 8px",
                              borderRadius: 6,
                              background: "#f3f4f6",
                              color: "#6b7280",
                              fontSize: 12,
                            }}
                          >
                            <XCircle size={13} />
                            중단됨
                          </span>
                        )}
                      </td>
                      <td style={{ padding: "14px 10px", color: "var(--sub)" }}>
                        {u.lastSentDate || "미발송"}
                      </td>
                      <td style={{ padding: "14px 10px", textAlign: "right" }}>
                        <div style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                          <button
                            title={u.isEnabled ? "알림 끄기" : "알림 켜기"}
                            disabled={isBusy}
                            onClick={() => handleToggle(u)}
                            style={{
                              padding: "6px 10px",
                              borderRadius: 6,
                              border: "1px solid var(--line)",
                              background: u.isEnabled ? "#fff" : "var(--ink)",
                              color: u.isEnabled ? "var(--ink)" : "#fff",
                              fontSize: 12,
                              fontWeight: 500,
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                            }}
                          >
                            <Power size={12} />
                            {u.isEnabled ? "끄기" : "켜기"}
                          </button>
                          <button
                            title="오늘 발송 이력을 초기화하여 다시 알림이 가도록 설정"
                            disabled={isBusy}
                            onClick={() => handleResetSend(u)}
                            style={{
                              padding: "6px 10px",
                              borderRadius: 6,
                              border: "1px solid var(--line)",
                              background: "#fff",
                              color: "var(--ink)",
                              fontSize: 12,
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                            }}
                          >
                            <RotateCcw size={12} />
                            리셋
                          </button>
                          <button
                            title="연동 데이터 삭제"
                            disabled={isBusy}
                            onClick={() => handleDelete(u)}
                            style={{
                              padding: "6px 8px",
                              borderRadius: 6,
                              border: "1px solid var(--line)",
                              background: "#fff",
                              color: "#dc2626",
                              fontSize: 12,
                              display: "inline-flex",
                              alignItems: "center",
                            }}
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
};
