import { useEffect, useState } from "react";
import { Bell } from "lucide-react";
import { MealPage } from "./features/meals/MealPage";
import { GamesPage } from "./features/games/GamesPage";
import { RoomPage } from "./features/rooms/RoomPage";
import { SettingsPage } from "./features/settings/SettingsPage";

type Page = "meals" | "games" | "rooms" | "settings";
const pages = [
  { id: "meals", label: "식단" },
  { id: "games", label: "커피내기" },
  { id: "rooms", label: "메뉴 투표" },
] as const;
const initialPage = (): Page => {
  const params = new URLSearchParams(window.location.search);
  if (params.has("code") || params.has("welstory") || params.has("error"))
    return "settings";
  const hash = window.location.hash.slice(1);
  if (["meals", "games", "rooms", "settings"].includes(hash))
    return hash as Page;
  return params.has("room") || params.has("escape") ? "rooms" : "meals";
};
export const App = (): React.JSX.Element => {
  const [page, setPage] = useState<Page>(initialPage);
  useEffect(() => {
    const update = () => setPage(initialPage());
    window.addEventListener("hashchange", update);
    return () => window.removeEventListener("hashchange", update);
  }, []);
  const navigate = (next: Page) => {
    window.location.hash = next;
    setPage(next);
    window.scrollTo({ top: 0, behavior: "instant" });
  };
  return (
    <>
      <a className="skip-link" href="#main-content">
        본문으로 이동
      </a>
      <header className="header">
        <div className="header-inner">
          <a className="logo" href="#meals" onClick={() => navigate("meals")}>
            <span className="logo-mark">
              <i />
            </span>
            아무거나
          </a>
          <nav className="main-nav" aria-label="메인 메뉴">
            {pages.map((p) => (
              <button
                key={p.id}
                aria-current={page === p.id ? "page" : undefined}
                className={page === p.id ? "active" : ""}
                onClick={() => navigate(p.id)}
              >
                {p.label}
              </button>
            ))}
          </nav>
          <div className="header-end">
            <button
              className="icon-btn"
              aria-label="식단 알림 설정"
              onClick={() => navigate("settings")}
            >
              <Bell size={20} />
            </button>
          </div>
        </div>
      </header>
      <main id="main-content" className="container">
        {page === "meals" && (
          <MealPage
            onGames={() => navigate("games")}
            onSettings={() => navigate("settings")}
          />
        )}
        {page === "games" && <GamesPage />}
        {page === "rooms" && <RoomPage />}
        {page === "settings" && <SettingsPage />}
        <footer className="footer">
          <span>아무거나 버스터</span>
          <div className="footer-links">
            <button onClick={() => navigate("settings")}>식단 알림 설정</button>
          </div>
        </footer>
      </main>
    </>
  );
};
export default App;
