import { useEffect, useRef, useState } from "react";
import { Bell, Check, ArrowUpRight } from "lucide-react";
import { api, cafeterias, errorText, post } from "../../lib/api";
interface Settings {
  nickname: string;
  cotNo: string;
  hallNo: string;
  cafeteriaName: string;
  scheduledDays: string;
  scheduledTime: string;
  enabled: boolean;
  isEnabled?: boolean;
}
export const SettingsPage = (): React.JSX.Element => {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [busy, setBusy] = useState(true);
  const [message, setMessage] = useState("");
  const started = useRef(false);
  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const load = code
      ? post<Settings>("/welstory/token-exchange", {
          code,
          state: params.get("state"),
          redirectUri: window.location.origin,
        })
      : api<Settings>("/welstory/settings");
    if (code || params.has("error"))
      window.history.replaceState(
        {},
        "",
        `${window.location.pathname}#settings`,
      );
    load
      .then(setSettings)
      .catch((e) => {
        if (code || errorText(e) !== "카카오 연결이 필요합니다.")
          setMessage(errorText(e));
      })
      .finally(() => setBusy(false));
  }, []);
  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setMessage("");
    try {
      await action();
    } catch (e) {
      setMessage(errorText(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <section className="page-enter">
      <div className="section-heading">
        <div>
          <span className="eyebrow">A LITTLE REMINDER</span>
          <h1>식단 알림</h1>
          <p>카카오톡으로 받을 식단과 시간을 설정합니다.</p>
        </div>
        <Bell size={42} strokeWidth={1} />
      </div>
      <div className="settings-layout">
        <div className="settings-card">
          {!settings ? (
            <div className="connect-card">
              <span className="eyebrow">KAKAO TALK</span>
              <h2>카카오톡 연결</h2>
              <p>계정을 연결한 후 식당과 발송 시간을 선택하세요.</p>
              <button
                className="button kakao"
                disabled={busy}
                onClick={() =>
                  run(async () => {
                    const data = await api<{ url: string }>(
                      `/welstory/auth-url?redirectUri=${encodeURIComponent(window.location.origin)}`,
                    );
                    window.location.assign(data.url);
                  })
                }
              >
                {busy ? "연결 확인 중…" : "카카오로 연결하기"}
                <ArrowUpRight size={18} />
              </button>
            </div>
          ) : (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                run(async () => {
                  const saved = await post<Settings>("/welstory/settings", {
                    ...settings,
                    isEnabled: settings.enabled,
                  });
                  setSettings(saved);
                  setMessage("알림 설정을 저장했습니다.");
                });
              }}
            >
              <div className="connected">
                <Check size={17} />
                {settings.nickname} 님의 카카오톡 연결됨
              </div>
              <label className="field-label">
                구내식당
                <select
                  value={settings.cotNo}
                  onChange={(e) => {
                    const c = cafeterias.find(
                      (item) => item.cotNo === e.target.value,
                    )!;
                    setSettings({
                      ...settings,
                      cotNo: c.cotNo,
                      hallNo: c.hallNo,
                      cafeteriaName: c.name,
                    });
                  }}
                >
                  {cafeterias.map((c) => (
                    <option key={c.cotNo} value={c.cotNo}>
                      {c.short}
                    </option>
                  ))}
                </select>
              </label>
              <fieldset className="day-field">
                <legend>받을 요일</legend>
                <div className="days">
                  {["월", "화", "수", "목", "금", "토", "일"].map((day, i) => {
                    const selected = settings.scheduledDays
                      .split(",")
                      .includes(String(i + 1));
                    return (
                      <button
                        type="button"
                        key={day}
                        aria-pressed={selected}
                        className={selected ? "active" : ""}
                        onClick={() => {
                          const days = settings.scheduledDays
                            .split(",")
                            .filter(Boolean);
                          setSettings({
                            ...settings,
                            scheduledDays: (selected
                              ? days.filter((d) => d !== String(i + 1))
                              : [...days, String(i + 1)]
                            )
                              .sort()
                              .join(","),
                          });
                        }}
                      >
                        {day}
                      </button>
                    );
                  })}
                </div>
              </fieldset>
              <label className="field-label">
                받을 시간
                <input
                  type="time"
                  required
                  value={settings.scheduledTime}
                  onChange={(e) =>
                    setSettings({ ...settings, scheduledTime: e.target.value })
                  }
                />
              </label>
              <label className="toggle-field">
                <span>
                  <strong>식단 알림 받기</strong>
                  <small>설정한 요일과 시간에 발송합니다.</small>
                </span>
                <input
                  type="checkbox"
                  checked={settings.enabled}
                  onChange={(e) =>
                    setSettings({ ...settings, enabled: e.target.checked })
                  }
                />
              </label>
              <div className="button-row">
                <button
                  className="button primary"
                  disabled={busy || !settings.scheduledDays}
                >
                  {busy ? "처리 중…" : "설정 저장"}
                </button>
                <button
                  type="button"
                  className="button secondary"
                  disabled={busy}
                  onClick={() =>
                    run(async () => {
                      await post("/welstory/test-send", {});
                      setMessage("저장된 설정으로 식단을 발송했습니다.");
                    })
                  }
                >
                  저장된 식단 받아보기
                </button>
              </div>
              <button
                type="button"
                className="text-button"
                disabled={busy}
                onClick={() =>
                  run(async () => {
                    await post("/welstory/logout", {});
                    setSettings(null);
                    setMessage("알림을 끄고 로그아웃했습니다.");
                  })
                }
              >
                알림 끄고 로그아웃
              </button>
            </form>
          )}
          {message && (
            <p className="notice" role="status">
              {message}
            </p>
          )}
        </div>
        <aside className="rules-panel">
          <span className="eyebrow">GOOD TO KNOW</span>
          <h3>내 점심에 맞춰서.</h3>
          <p>
            오전 9시 전에는 아침,
            <br />
            오후 1시 전에는 점심,
            <br />그 이후에는 저녁 식단을 보내요.
          </p>
          <p className="muted">
            새로 바꾼 시간과 요일은 저장해야 적용돼요. 식단을 확인할 수 없는
            날에는 임의의 메뉴를 보내지 않아요.
          </p>
        </aside>
      </div>
    </section>
  );
};
